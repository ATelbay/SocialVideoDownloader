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

    // Map requestId → NSURLSessionDownloadTask so we can cancel by ID.
    private val activeTasks = mutableMapOf<String, NSURLSessionDownloadTask>()

    // Map taskIdentifier (Int) → DownloadRequest so delegate callbacks can reconstruct context.
    private val taskRequests = mutableMapOf<Long, DownloadRequest>()

    private val delegate = DownloadSessionDelegate()
    private val session: NSURLSession by lazy {
        val config =
            NSURLSessionConfiguration.backgroundSessionConfigurationWithIdentifier(
                BACKGROUND_SESSION_ID,
            )
        config.timeoutIntervalForRequest = 30.0
        config.timeoutIntervalForResource = 3600.0 // 1 hour for large videos
        NSURLSession.sessionWithConfiguration(config, delegate = delegate, delegateQueue = null)
    }

    init {
        delegate.onProgress = { taskId, bytesWritten, totalWritten, totalExpected ->
            val request = taskRequests[taskId] ?: return@onProgress
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

        delegate.onCompleted = { taskId, tempUrl ->
            val request = taskRequests.remove(taskId) ?: return@onCompleted
            activeTasks.remove(request.id)
            activeRequestId = null

            scope.launch {
                try {
                    val destPath = moveToDownloads(tempUrl, request.videoTitle, request.formatId)
                    _downloadState.value =
                        DownloadServiceState.Completed(
                            requestId = request.id,
                            filePath = destPath,
                            fileUri = null,
                        )
                } catch (e: Exception) {
                    _downloadState.value =
                        DownloadServiceState.Failed(
                            requestId = request.id,
                            error = DownloadErrorType.STORAGE_FULL,
                        )
                }
            }
        }

        delegate.onFailed = { taskId, error ->
            val request = taskRequests.remove(taskId) ?: return@onFailed
            activeTasks.remove(request.id)
            if (activeRequestId == request.id) activeRequestId = null

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

        val task = session.downloadTaskWithURL(url)
        val taskId = task.taskIdentifier.toLong()

        activeTasks[request.id] = task
        taskRequests[taskId] = request
        activeRequestId = request.id

        _downloadState.value =
            DownloadServiceState.Queued(
                requestId = request.id,
                videoTitle = request.videoTitle,
            )

        task.resume()
    }

    override fun cancelDownload(requestId: String) {
        val task = activeTasks.remove(requestId) ?: return
        task.cancel()
        // Delegate onFailed with NSURLErrorCancelled (-999) handles state update.
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
