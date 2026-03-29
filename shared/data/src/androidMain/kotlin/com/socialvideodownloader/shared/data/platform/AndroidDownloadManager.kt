package com.socialvideodownloader.shared.data.platform

import android.content.Context
import android.content.Intent
import com.socialvideodownloader.core.domain.model.DownloadRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Android implementation of [PlatformDownloadManager].
 *
 * This is a bridge to the existing DownloadService and DownloadServiceStateHolder
 * in feature/download. The actual download execution is handled by the foreground
 * service; this class simply translates between the shared interface and Android
 * service intents.
 *
 * Full integration with DownloadService happens in Phase 5 when the shared
 * ViewModel delegates to this manager. For now, the state is managed locally
 * and the start/cancel methods are stubs that will be wired to the service.
 */
class AndroidDownloadManager(
    private val context: Context,
) : PlatformDownloadManager {

    private val _downloadState = MutableStateFlow<DownloadServiceState>(DownloadServiceState.Idle)
    override val downloadState: StateFlow<DownloadServiceState> = _downloadState.asStateFlow()

    override val activeRequestId: String?
        get() = when (val state = _downloadState.value) {
            is DownloadServiceState.Queued -> state.requestId
            is DownloadServiceState.Downloading -> state.requestId
            else -> null
        }

    override suspend fun startDownload(request: DownloadRequest) {
        // TODO: Wire to DownloadService via startForegroundService Intent.
        // This will be connected in Phase 5 (T076) when DownloadViewModel
        // delegates to SharedDownloadViewModel which uses this manager.
        _downloadState.value = DownloadServiceState.Queued(
            requestId = request.id,
            videoTitle = request.videoTitle,
        )
    }

    override fun cancelDownload(requestId: String) {
        // TODO: Send cancel Intent to DownloadService.
        _downloadState.value = DownloadServiceState.Cancelled(requestId)
    }

    /**
     * Update the download state from the existing DownloadServiceStateHolder.
     * Called by the Hilt bridge to sync Android service state with the shared interface.
     */
    fun updateState(newState: DownloadServiceState) {
        _downloadState.value = newState
    }
}
