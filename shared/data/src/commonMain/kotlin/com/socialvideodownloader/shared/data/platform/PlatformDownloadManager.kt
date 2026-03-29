package com.socialvideodownloader.shared.data.platform

import com.socialvideodownloader.core.domain.model.DownloadProgress
import com.socialvideodownloader.core.domain.model.DownloadRequest
import kotlinx.coroutines.flow.StateFlow

/**
 * Platform abstraction for download execution.
 *
 * Android: Wraps DownloadServiceStateHolder + foreground service.
 * iOS: Uses URLSession background download tasks.
 */
interface PlatformDownloadManager {

    /** Start a download for the given request. */
    suspend fun startDownload(request: DownloadRequest)

    /** Cancel an in-progress download by request ID. */
    fun cancelDownload(requestId: String)

    /** Observable stream of download state changes. */
    val downloadState: StateFlow<DownloadServiceState>

    /** The currently active download request ID, or null if idle. */
    val activeRequestId: String?
}

/** Download service state machine shared across platforms. */
sealed interface DownloadServiceState {
    data object Idle : DownloadServiceState

    data class Queued(
        val requestId: String,
        val videoTitle: String,
    ) : DownloadServiceState

    data class Downloading(
        val requestId: String,
        val progress: DownloadProgress,
    ) : DownloadServiceState

    data class Completed(
        val requestId: String,
        val filePath: String,
        val fileUri: String? = null,
    ) : DownloadServiceState

    data class Failed(
        val requestId: String,
        val error: DownloadErrorType,
    ) : DownloadServiceState

    data class Cancelled(
        val requestId: String,
    ) : DownloadServiceState
}

/** Error types for download failures — replaces @StringRes pattern. */
enum class DownloadErrorType {
    NETWORK_ERROR,
    SERVER_UNAVAILABLE,
    EXTRACTION_FAILED,
    UNSUPPORTED_URL,
    STORAGE_FULL,
    DOWNLOAD_FAILED,
    UNKNOWN,
}
