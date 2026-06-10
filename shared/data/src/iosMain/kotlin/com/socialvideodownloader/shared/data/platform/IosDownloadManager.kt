package com.socialvideodownloader.shared.data.platform

import com.socialvideodownloader.core.domain.model.DownloadProgress
import com.socialvideodownloader.core.domain.model.DownloadRequest
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSError
import platform.Foundation.NSFileManager
import platform.Foundation.NSLock
import platform.Foundation.NSLog
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSURLSession
import platform.Foundation.NSURLSessionConfiguration
import platform.Foundation.NSURLSessionDownloadDelegateProtocol
import platform.Foundation.NSURLSessionDownloadTask
import platform.Foundation.NSURLSessionTask
import platform.Foundation.NSUserDomainMask
import platform.darwin.NSObject

private const val BACKGROUND_SESSION_ID = "com.socialvideodownloader.ios.download"
private const val SVD_DIRECTORY = "SocialVideoDownloader"
private const val BACKGROUND_SESSION_UNAVAILABLE_CODE = 4097L
private const val NS_URL_ERROR_UNKNOWN = -1L
private const val MUX_STAGING_DIRECTORY = "svd_mux"
private const val MUX_AUDIO_FILE_NAME = "audio.m4a"

// AVFoundation cannot read webm, so on-device muxing is limited to mp4/m4v containers;
// webm video-only formats keep the existing single-stream (no audio) behavior.
private val MUX_COMPATIBLE_EXTS = setOf("mp4", "m4v")

/**
 * iOS implementation of [PlatformDownloadManager] using NSURLSession background downloads.
 *
 * - Uses a background NSURLSession so downloads survive app suspension.
 * - Progress is tracked via [NSURLSessionDownloadDelegateProtocol].
 * - On completion the file is moved from the system temp location to
 *   `Documents/SocialVideoDownloader/`.
 * - State is exposed as a [StateFlow] consumed by [SharedDownloadViewModel].
 * - Video-only mp4/m4v formats with [DownloadRequest.audioDirectUrl] download both streams
 *   sequentially and merge them on-device via [IosStreamMuxer]; webm stays single-stream
 *   (AVFoundation cannot read webm). If the app is killed between the two stages the second
 *   stage is lost — the same in-memory-registry limitation as single-stream downloads.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class IosDownloadManager : PlatformDownloadManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _downloadState = MutableStateFlow<DownloadServiceState>(DownloadServiceState.Idle)
    override val downloadState: StateFlow<DownloadServiceState> = _downloadState.asStateFlow()

    override var activeRequestId: String? = null
        private set

    private enum class SessionMode {
        BACKGROUND,
        FOREGROUND,
    }

    private enum class MuxStage {
        VIDEO,
        AUDIO,
    }

    private class ActiveDownload(
        val request: DownloadRequest,
        var task: NSURLSessionDownloadTask,
        val sessionMode: SessionMode,
        var hasReportedProgress: Boolean = false,
        val isMux: Boolean = false,
        var stage: MuxStage = MuxStage.VIDEO,
        var stagedVideoUrl: NSURL? = null,
    )

    // Map requestId → active download so we can cancel by ID.
    private val activeDownloadsByRequestId = mutableMapOf<String, ActiveDownload>()

    // Map taskIdentifier → active download so delegate callbacks can reconstruct context.
    private val activeDownloadsByTaskId = mutableMapOf<Long, ActiveDownload>()

    // NSURLSession delegate callbacks arrive on the session's operation queue while
    // start/cancel run on coroutine threads, so every access to the two maps above is
    // serialized through this lock to avoid concurrent-mutation data races.
    private val stateLock = NSLock()

    private inline fun <T> withStateLock(block: () -> T): T {
        stateLock.lock()
        try {
            return block()
        } finally {
            stateLock.unlock()
        }
    }

    private fun registerDownload(active: ActiveDownload) =
        withStateLock {
            activeDownloadsByRequestId[active.request.id] = active
            activeDownloadsByTaskId[active.task.taskIdentifier.toLong()] = active
        }

    /** Reads the active download for a task without removing it. */
    private fun peekByTaskId(taskId: Long): ActiveDownload? = withStateLock { activeDownloadsByTaskId[taskId] }

    /** Removes and returns the active download for a task, clearing both maps. */
    private fun removeByTaskId(taskId: Long): ActiveDownload? =
        withStateLock {
            val active = activeDownloadsByTaskId.remove(taskId)
            if (active != null) activeDownloadsByRequestId.remove(active.request.id)
            active
        }

    /** Removes and returns the active download for a request id, clearing both maps. */
    private fun removeByRequestId(requestId: String): ActiveDownload? =
        withStateLock {
            val active = activeDownloadsByRequestId.remove(requestId)
            if (active != null) activeDownloadsByTaskId.remove(active.task.taskIdentifier.toLong())
            active
        }

    private val streamMuxer = IosStreamMuxer()

    private val delegate = DownloadSessionDelegate()
    private val backgroundSession: NSURLSession by lazy {
        val config =
            NSURLSessionConfiguration.backgroundSessionConfigurationWithIdentifier(
                BACKGROUND_SESSION_ID,
            )
        config.timeoutIntervalForRequest = 30.0
        config.timeoutIntervalForResource = 3600.0 // 1 hour for large videos
        NSURLSession.sessionWithConfiguration(config, delegate = delegate, delegateQueue = null)
    }
    private val foregroundSession: NSURLSession by lazy {
        val config = NSURLSessionConfiguration.defaultSessionConfiguration()
        config.timeoutIntervalForRequest = 30.0
        config.timeoutIntervalForResource = 3600.0
        NSURLSession.sessionWithConfiguration(config, delegate = delegate, delegateQueue = null)
    }

    init {
        delegate.onProgress = onProgress@{ taskId, bytesWritten, totalWritten, totalExpected ->
            val activeDownload = peekByTaskId(taskId) ?: return@onProgress
            activeDownload.hasReportedProgress = true
            val request = activeDownload.request
            val rawProgress =
                if (totalExpected > 0) {
                    totalWritten.toFloat() / totalExpected.toFloat()
                } else {
                    -1f
                }
            val progress =
                if (activeDownload.isMux) {
                    // Two-stream downloads map each stream into its segment of the overall
                    // progress; the remaining 0.95..1 is the on-device mux + finalize.
                    when (activeDownload.stage) {
                        MuxStage.VIDEO -> scaleMuxProgress(rawProgress, 0f, MUX_VIDEO_PROGRESS_END)
                        MuxStage.AUDIO ->
                            scaleMuxProgress(rawProgress, MUX_VIDEO_PROGRESS_END, MUX_AUDIO_PROGRESS_END)
                    }
                } else {
                    rawProgress
                }
            val speedBytesPerSec = bytesWritten // crude approximation per callback
            _downloadState.value =
                DownloadServiceState.Downloading(
                    requestId = request.id,
                    progress =
                        DownloadProgress(
                            requestId = request.id,
                            progressPercent = progress.coerceIn(0f, 1f),
                            downloadedBytes = totalWritten,
                            totalBytes = if (totalExpected > 0) totalExpected else null,
                            speedBytesPerSec = speedBytesPerSec,
                            etaSeconds =
                                if (totalExpected > 0 && speedBytesPerSec > 0) {
                                    (totalExpected - totalWritten) / speedBytesPerSec
                                } else {
                                    0L
                                },
                        ),
                )
        }

        delegate.onCompleted = onCompleted@{ taskId, tempUrl ->
            val activeDownload = removeByTaskId(taskId) ?: return@onCompleted
            val request = activeDownload.request
            log(
                "download_completed requestId=${request.id} " +
                    "mode=${activeDownload.sessionMode.name.lowercase()} " +
                    "stage=${if (activeDownload.isMux) activeDownload.stage.name.lowercase() else "single"} " +
                    "title=${request.videoTitle}",
            )

            if (activeDownload.isMux) {
                // The download stays active across the audio stage and the mux, so
                // activeRequestId is cleared inside the mux handlers instead of here.
                handleMuxStageCompleted(activeDownload, tempUrl)
                return@onCompleted
            }

            activeRequestId = null
            try {
                // URLSession's temporary file may be removed as soon as this callback returns,
                // so finalize it synchronously while the location is still valid.
                val destPath = moveToDownloads(tempUrl, request.videoTitle, request.ext)
                _downloadState.value =
                    DownloadServiceState.Completed(
                        requestId = request.id,
                        filePath = destPath,
                        fileUri = "file://$destPath",
                    )
            } catch (e: Exception) {
                log(
                    "download_finalize_failed requestId=${request.id} " +
                        "mode=${activeDownload.sessionMode.name.lowercase()} " +
                        "tempPath=${tempUrl.path ?: "unknown"} message=${e.message ?: "unknown"}",
                )
                _downloadState.value =
                    DownloadServiceState.Failed(
                        requestId = request.id,
                        error = classifyFinalizeError(e),
                    )
            }
        }

        delegate.onFailed = onFailed@{ taskId, error ->
            val activeDownload = removeByTaskId(taskId) ?: return@onFailed
            val request = activeDownload.request
            if (activeRequestId == request.id) activeRequestId = null
            // The foreground fallback below restarts from the VIDEO stage, so the staging
            // directory is safe to drop on any failure path.
            if (activeDownload.isMux) cleanupMuxStaging(request.id)

            val code = error?.code?.toLong()
            val domain = error?.domain ?: "unknown"
            val description = error?.localizedDescription ?: "unknown"
            log(
                "download_failed requestId=${request.id} mode=${activeDownload.sessionMode.name.lowercase()} " +
                    "domain=$domain code=$code progressSeen=${activeDownload.hasReportedProgress} description=$description",
            )

            if (shouldFallbackToForeground(activeDownload, error)) {
                log(
                    "background_unavailable -> falling_back_to_foreground " +
                        "requestId=${request.id} code=$code domain=$domain description=$description",
                )
                scope.launch {
                    startDownloadInternal(request, SessionMode.FOREGROUND)
                }
                return@onFailed
            }

            val errorType =
                when {
                    error?.code?.toInt() == -999 -> {
                        // NSURLErrorCancelled — task was cancelled intentionally
                        _downloadState.value = DownloadServiceState.Cancelled(requestId = request.id)
                        return@onFailed
                    }
                    (error?.code?.toInt() ?: 0) in -1009..-1000 -> DownloadErrorType.NETWORK_ERROR
                    else -> DownloadErrorType.DOWNLOAD_FAILED
                }
            _downloadState.value =
                DownloadServiceState.Failed(
                    requestId = request.id,
                    error = errorType,
                )
        }
    }

    override suspend fun startDownload(request: DownloadRequest) {
        val downloadUrl =
            request.directDownloadUrl
                ?: throw IllegalStateException(
                    "IosDownloadManager requires a directDownloadUrl. " +
                        "Ensure the server provides download URLs in the extract response.",
                )

        val url =
            NSURL.URLWithString(downloadUrl)
                ?: throw IllegalStateException("Invalid download URL: $downloadUrl")

        _downloadState.value =
            DownloadServiceState.Queued(
                requestId = request.id,
                videoTitle = request.videoTitle,
            )

        val sessionMode =
            if (isSimulator()) {
                SessionMode.FOREGROUND
            } else {
                SessionMode.BACKGROUND
            }

        log(
            "download_start requestId=${request.id} mode=${sessionMode.name.lowercase()} " +
                "title=${request.videoTitle} directUrlPresent=${request.directDownloadUrl != null}",
        )
        startDownloadInternal(request, sessionMode, prevalidatedUrl = url)
    }

    override fun cancelDownload(requestId: String) {
        val activeDownload = removeByRequestId(requestId) ?: return
        activeDownload.task.cancel()
        if (activeDownload.isMux) cleanupMuxStaging(requestId)
        // Delegate onFailed with NSURLErrorCancelled (-999) handles state update.
    }

    private fun isSimulator(): Boolean {
        val environment = NSProcessInfo.processInfo.environment
        return environment["SIMULATOR_DEVICE_NAME"] != null ||
            environment["SIMULATOR_MODEL_IDENTIFIER"] != null ||
            environment["SIMULATOR_UDID"] != null ||
            environment["SIMULATOR_ROOT"] != null ||
            environment["IPHONE_SIMULATOR_ROOT"] != null
    }

    private suspend fun startDownloadInternal(
        request: DownloadRequest,
        sessionMode: SessionMode,
        prevalidatedUrl: NSURL? = null,
    ) {
        val downloadUrl =
            request.directDownloadUrl
                ?: throw IllegalStateException(
                    "IosDownloadManager requires a directDownloadUrl. " +
                        "Ensure the server provides download URLs in the extract response.",
                )

        val url =
            prevalidatedUrl
                ?: NSURL.URLWithString(downloadUrl)
                ?: throw IllegalStateException("Invalid download URL: $downloadUrl")

        val session =
            when (sessionMode) {
                SessionMode.BACKGROUND -> backgroundSession
                SessionMode.FOREGROUND -> foregroundSession
            }

        val task = session.downloadTaskWithURL(url)
        val activeDownload =
            ActiveDownload(
                request = request,
                task = task,
                sessionMode = sessionMode,
                isMux = shouldMux(request),
            )
        registerDownload(activeDownload)
        activeRequestId = request.id
        task.resume()
    }

    // --- Two-stream (video + audio) mux support ---

    /**
     * Single decision point for the two-stream path: the shared ViewModel provides
     * [DownloadRequest.audioDirectUrl] for video-only formats, and AVFoundation can only
     * mux mp4/m4v containers (webm stays single-stream, a known iOS limitation).
     */
    private fun shouldMux(request: DownloadRequest): Boolean =
        request.audioDirectUrl != null && request.ext.lowercase() in MUX_COMPATIBLE_EXTS

    /**
     * Runs synchronously inside the delegate callback: NSURLSession's temp file is only
     * guaranteed to exist until the callback returns, so each stage's file is staged
     * (moved into our own temp directory) before anything asynchronous happens.
     */
    private fun handleMuxStageCompleted(
        activeDownload: ActiveDownload,
        tempUrl: NSURL,
    ) {
        val request = activeDownload.request
        try {
            when (activeDownload.stage) {
                MuxStage.VIDEO -> {
                    activeDownload.stagedVideoUrl =
                        stageMuxFile(tempUrl, request.id, "video.${request.ext}")
                    startAudioStage(activeDownload)
                }
                MuxStage.AUDIO -> {
                    val stagedAudioUrl = stageMuxFile(tempUrl, request.id, MUX_AUDIO_FILE_NAME)
                    val stagedVideoUrl = activeDownload.stagedVideoUrl
                    scope.launch { finishMux(request, stagedVideoUrl, stagedAudioUrl) }
                }
            }
        } catch (e: Exception) {
            log(
                "mux_stage_failed requestId=${request.id} " +
                    "stage=${activeDownload.stage.name.lowercase()} message=${e.message ?: "unknown"}",
            )
            cleanupMuxStaging(request.id)
            if (activeRequestId == request.id) activeRequestId = null
            _downloadState.value =
                DownloadServiceState.Failed(
                    requestId = request.id,
                    error = classifyFinalizeError(e),
                )
        }
    }

    private fun startAudioStage(activeDownload: ActiveDownload) {
        val request = activeDownload.request
        val audioUrlString =
            request.audioDirectUrl
                ?: throw StorageException("audioDirectUrl missing for mux download")
        val audioUrl =
            NSURL.URLWithString(audioUrlString)
                ?: throw StorageException("Invalid audio URL: $audioUrlString")
        val session =
            when (activeDownload.sessionMode) {
                SessionMode.BACKGROUND -> backgroundSession
                SessionMode.FOREGROUND -> foregroundSession
            }
        // The video-stage entry was already removed from both maps by onCompleted, so
        // re-registering under the new taskIdentifier is the only bookkeeping needed.
        activeDownload.task = session.downloadTaskWithURL(audioUrl)
        activeDownload.stage = MuxStage.AUDIO
        registerDownload(activeDownload)
        activeDownload.task.resume()
        log("mux_audio_stage_started requestId=${request.id}")
    }

    private suspend fun finishMux(
        request: DownloadRequest,
        stagedVideoUrl: NSURL?,
        stagedAudioUrl: NSURL,
    ) {
        try {
            val videoUrl = stagedVideoUrl ?: throw StorageException("Staged video stream is missing")
            val outputUrl =
                muxStagingDir(request.id)?.URLByAppendingPathComponent("out.${request.ext}")
                    ?: throw StorageException("Cannot build mux output URL")
            val muxed = streamMuxer.mux(videoUrl, stagedAudioUrl, outputUrl)
            if (!muxed) {
                // Keep the video-only stream rather than failing the whole download —
                // parity with the Android fallback for streams the platform muxer rejects.
                log("mux_failed requestId=${request.id} -> falling_back_to_video_only")
            }
            val destPath = moveToDownloads(if (muxed) outputUrl else videoUrl, request.videoTitle, request.ext)
            _downloadState.value =
                DownloadServiceState.Completed(
                    requestId = request.id,
                    filePath = destPath,
                    fileUri = "file://$destPath",
                )
        } catch (e: Exception) {
            log("mux_finalize_failed requestId=${request.id} message=${e.message ?: "unknown"}")
            _downloadState.value =
                DownloadServiceState.Failed(
                    requestId = request.id,
                    error = classifyFinalizeError(e),
                )
        } finally {
            if (activeRequestId == request.id) activeRequestId = null
            cleanupMuxStaging(request.id)
        }
    }

    private fun stageMuxFile(
        tempUrl: NSURL,
        requestId: String,
        fileName: String,
    ): NSURL {
        val dir =
            muxStagingDir(requestId)
                ?: throw StorageException("Cannot resolve mux staging directory")
        ensureDirectory(dir)
        val destUrl =
            dir.URLByAppendingPathComponent(fileName)
                ?: throw StorageException("Cannot build mux staging URL")

        val fileManager = NSFileManager.defaultManager
        destUrl.path?.let { path ->
            if (fileManager.fileExistsAtPath(path)) {
                fileManager.removeItemAtURL(destUrl, error = null)
            }
        }
        memScoped {
            val moveError = alloc<ObjCObjectVar<NSError?>>()
            if (!fileManager.moveItemAtURL(tempUrl, toURL = destUrl, error = moveError.ptr)) {
                val copyError = alloc<ObjCObjectVar<NSError?>>()
                if (!fileManager.copyItemAtURL(tempUrl, toURL = destUrl, error = copyError.ptr)) {
                    throw StorageException(
                        "Failed to stage mux stream " +
                            "(move: ${moveError.value?.localizedDescription ?: "unknown"}; " +
                            "copy: ${copyError.value?.localizedDescription ?: "unknown"})",
                    )
                }
                fileManager.removeItemAtURL(tempUrl, error = null)
            }
        }
        return destUrl
    }

    private fun muxStagingDir(requestId: String): NSURL? =
        NSURL.fileURLWithPath(NSTemporaryDirectory())
            .URLByAppendingPathComponent(MUX_STAGING_DIRECTORY)
            ?.URLByAppendingPathComponent(requestId)

    private fun cleanupMuxStaging(requestId: String) {
        val dir = muxStagingDir(requestId) ?: return
        NSFileManager.defaultManager.removeItemAtURL(dir, error = null)
    }

    private fun shouldFallbackToForeground(
        activeDownload: ActiveDownload,
        error: NSError?,
    ): Boolean {
        if (activeDownload.sessionMode != SessionMode.BACKGROUND) return false
        if (activeDownload.hasReportedProgress) return false

        val domain = error?.domain.orEmpty()
        val code = error?.code?.toLong()
        val description = error?.localizedDescription.orEmpty()
        val userInfoDescription = error?.userInfo?.toString().orEmpty()
        val combinedMessage = "$description $userInfoDescription"

        return code == BACKGROUND_SESSION_UNAVAILABLE_CODE ||
            code == NS_URL_ERROR_UNKNOWN ||
            domain.contains("NSCocoaErrorDomain", ignoreCase = true) &&
            code == BACKGROUND_SESSION_UNAVAILABLE_CODE ||
            combinedMessage.contains("nsurlsessiond", ignoreCase = true) ||
            combinedMessage.contains("remote session is unavailable", ignoreCase = true) ||
            combinedMessage.contains("background NSURLSessionDownloadTask", ignoreCase = true)
    }

    private fun log(message: String) {
        NSLog("SVD_IOS_DOWNLOAD $message")
    }

    private fun classifyFinalizeError(error: Exception): DownloadErrorType {
        val message = error.message.orEmpty()
        return if (
            message.contains("space", ignoreCase = true) ||
            message.contains("disk full", ignoreCase = true) ||
            message.contains("no space", ignoreCase = true)
        ) {
            DownloadErrorType.STORAGE_FULL
        } else {
            DownloadErrorType.DOWNLOAD_FAILED
        }
    }

    // --- File helpers ---

    private fun moveToDownloads(
        tempUrl: NSURL,
        videoTitle: String,
        ext: String,
    ): String {
        val destDir =
            svdDirectory()
                ?: throw StorageException("Cannot resolve Documents directory")

        ensureDirectory(destDir)

        val safeTitle = FileNames.sanitize(videoTitle, maxLength = 100)
        val safeExt = ext.takeIf { it.isNotEmpty() } ?: "mp4"
        val fileName = "$safeTitle.$safeExt"
        val destUrl =
            (
                destDir.URLByAppendingPathComponent(fileName)
                    ?: throw StorageException("Cannot build destination URL")
            )
                .let { uniqueUrl(it) }

        val fileManager = NSFileManager.defaultManager
        memScoped {
            val moveError = alloc<ObjCObjectVar<NSError?>>()
            if (!fileManager.moveItemAtURL(tempUrl, toURL = destUrl, error = moveError.ptr)) {
                val copyError = alloc<ObjCObjectVar<NSError?>>()
                if (!fileManager.copyItemAtURL(tempUrl, toURL = destUrl, error = copyError.ptr)) {
                    throw StorageException(
                        "Failed to move downloaded file to Documents " +
                            "(move: ${moveError.value?.localizedDescription ?: "unknown"}; " +
                            "copy: ${copyError.value?.localizedDescription ?: "unknown"})",
                    )
                }
                fileManager.removeItemAtURL(tempUrl, error = null)
            }
        }

        return destUrl.path ?: throw StorageException("Destination path is nil after move")
    }

    private fun svdDirectory(): NSURL? {
        val docDir =
            NSFileManager.defaultManager.URLForDirectory(
                directory = NSDocumentDirectory,
                inDomain = NSUserDomainMask,
                appropriateForURL = null,
                create = true,
                error = null,
            ) ?: return null
        return docDir.URLByAppendingPathComponent(SVD_DIRECTORY)
    }

    private fun ensureDirectory(url: NSURL) {
        val path = url.path ?: return
        if (!NSFileManager.defaultManager.fileExistsAtPath(path)) {
            NSFileManager.defaultManager.createDirectoryAtPath(
                path = path,
                withIntermediateDirectories = true,
                attributes = null,
                error = null,
            )
        }
    }

    private fun uniqueUrl(base: NSURL): NSURL {
        val fileManager = NSFileManager.defaultManager
        if (base.path?.let { fileManager.fileExistsAtPath(it) } != true) return base
        val dir = base.URLByDeletingLastPathComponent ?: base
        val name = base.lastPathComponent ?: "file"
        val dotIdx = name.lastIndexOf('.')
        val baseName = if (dotIdx >= 0) name.substring(0, dotIdx) else name
        val ext = if (dotIdx >= 0) name.substring(dotIdx) else ""
        var counter = 1
        while (true) {
            val candidate = dir.URLByAppendingPathComponent("$baseName ($counter)$ext") ?: break
            if (candidate.path?.let { fileManager.fileExistsAtPath(it) } != true) return candidate
            counter++
        }
        return base
    }
}

/**
 * Delegate that bridges NSURLSession callbacks into Kotlin lambdas.
 *
 * Extends NSObject so it can conform to NSURLSessionDownloadDelegateProtocol.
 */
@OptIn(ExperimentalForeignApi::class)
private class DownloadSessionDelegate : NSObject(), NSURLSessionDownloadDelegateProtocol {
    var onProgress: ((taskId: Long, bytesWritten: Long, totalWritten: Long, totalExpected: Long) -> Unit)? = null
    var onCompleted: ((taskId: Long, location: NSURL) -> Unit)? = null
    var onFailed: ((taskId: Long, error: NSError?) -> Unit)? = null

    override fun URLSession(
        session: NSURLSession,
        downloadTask: NSURLSessionDownloadTask,
        didWriteData: Long,
        totalBytesWritten: Long,
        totalBytesExpectedToWrite: Long,
    ) {
        onProgress?.invoke(
            downloadTask.taskIdentifier.toLong(),
            didWriteData,
            totalBytesWritten,
            totalBytesExpectedToWrite,
        )
    }

    override fun URLSession(
        session: NSURLSession,
        downloadTask: NSURLSessionDownloadTask,
        didFinishDownloadingToURL: NSURL,
    ) {
        onCompleted?.invoke(
            downloadTask.taskIdentifier.toLong(),
            didFinishDownloadingToURL,
        )
    }

    override fun URLSession(
        session: NSURLSession,
        task: NSURLSessionTask,
        didCompleteWithError: NSError?,
    ) {
        // Only invoke onFailed if the task actually failed (not just completed successfully).
        if (didCompleteWithError != null) {
            onFailed?.invoke(task.taskIdentifier.toLong(), didCompleteWithError)
        }
    }
}
