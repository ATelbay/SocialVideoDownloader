package com.socialvideodownloader.shared.data.platform

import com.socialvideodownloader.core.domain.model.DownloadProgress
import com.socialvideodownloader.core.domain.model.DownloadRequest
import kotlinx.cinterop.ExperimentalForeignApi
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
import platform.Foundation.NSLog
import platform.Foundation.NSProcessInfo
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

/**
 * iOS implementation of [PlatformDownloadManager] using NSURLSession background downloads.
 *
 * - Uses a background NSURLSession so downloads survive app suspension.
 * - Progress is tracked via [NSURLSessionDownloadDelegateProtocol].
 * - On completion the file is moved from the system temp location to
 *   `Documents/SocialVideoDownloader/`.
 * - State is exposed as a [StateFlow] consumed by [SharedDownloadViewModel].
 */
@OptIn(ExperimentalForeignApi::class)
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

    private class ActiveDownload(
        val request: DownloadRequest,
        val task: NSURLSessionDownloadTask,
        val sessionMode: SessionMode,
        var hasReportedProgress: Boolean = false,
    )

    // Map requestId → active download so we can cancel by ID.
    private val activeDownloadsByRequestId = mutableMapOf<String, ActiveDownload>()

    // Map taskIdentifier → active download so delegate callbacks can reconstruct context.
    private val activeDownloadsByTaskId = mutableMapOf<Long, ActiveDownload>()

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
            val activeDownload = activeDownloadsByTaskId[taskId] ?: return@onProgress
            activeDownload.hasReportedProgress = true
            val request = activeDownload.request
            val progress =
                if (totalExpected > 0) {
                    totalWritten.toFloat() / totalExpected.toFloat()
                } else {
                    -1f
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
            val activeDownload = activeDownloadsByTaskId.remove(taskId) ?: return@onCompleted
            val request = activeDownload.request
            activeDownloadsByRequestId.remove(request.id)
            activeRequestId = null
            log(
                "download_completed requestId=${request.id} " +
                    "mode=${activeDownload.sessionMode.name.lowercase()} title=${request.videoTitle}",
            )

            try {
                // URLSession's temporary file may be removed as soon as this callback returns,
                // so finalize it synchronously while the location is still valid.
                val destPath = moveToDownloads(tempUrl, request.videoTitle, request.formatId)
                _downloadState.value =
                    DownloadServiceState.Completed(
                        requestId = request.id,
                        filePath = destPath,
                        fileUri = null,
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
            val activeDownload = activeDownloadsByTaskId.remove(taskId) ?: return@onFailed
            val request = activeDownload.request
            activeDownloadsByRequestId.remove(request.id)
            if (activeRequestId == request.id) activeRequestId = null

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
        val activeDownload = activeDownloadsByRequestId.remove(requestId) ?: return
        activeDownloadsByTaskId.remove(activeDownload.task.taskIdentifier.toLong())
        activeDownload.task.cancel()
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
        val activeDownload = ActiveDownload(request = request, task = task, sessionMode = sessionMode)
        activeDownloadsByRequestId[request.id] = activeDownload
        activeDownloadsByTaskId[task.taskIdentifier.toLong()] = activeDownload
        activeRequestId = request.id
        task.resume()
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
        formatId: String,
    ): String {
        val destDir =
            svdDirectory()
                ?: throw StorageException("Cannot resolve Documents directory")

        ensureDirectory(destDir)

        val safeTitle = sanitizeFileName(videoTitle).take(100)
        val ext = formatId.substringAfterLast('.', "mp4")
        val fileName = "$safeTitle.$ext"
        val destUrl =
            (
                destDir.URLByAppendingPathComponent(fileName)
                    ?: throw StorageException("Cannot build destination URL")
            )
                .let { uniqueUrl(it) }

        val fileManager = NSFileManager.defaultManager
        val moved = fileManager.moveItemAtURL(tempUrl, toURL = destUrl, error = null)
        if (!moved) {
            val copied = fileManager.copyItemAtURL(tempUrl, toURL = destUrl, error = null)
            if (!copied) throw StorageException("Failed to move downloaded file to Documents")
            fileManager.removeItemAtURL(tempUrl, error = null)
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

    private fun sanitizeFileName(name: String): String {
        val cleaned = name.replace(Regex("[/\\\\:*?\"<>|]"), "_").trim()
        return cleaned.ifEmpty { "download" }
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
