package com.socialvideodownloader.shared.data.platform

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
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
        get() =
            when (val state = _downloadState.value) {
                is DownloadServiceState.Queued -> state.requestId
                is DownloadServiceState.Downloading -> state.requestId
                else -> null
            }

    override suspend fun startDownload(request: DownloadRequest) {
        val intent =
            Intent(ACTION_START_DOWNLOAD).apply {
                component = ComponentName(context.packageName, DOWNLOAD_SERVICE_CLASS)
                putExtra(EXTRA_REQUEST_ID, request.id)
                putExtra(EXTRA_SOURCE_URL, request.sourceUrl)
                putExtra(EXTRA_VIDEO_TITLE, request.videoTitle)
                putExtra(EXTRA_THUMBNAIL_URL, request.thumbnailUrl)
                putExtra(EXTRA_FORMAT_ID, request.formatId)
                putExtra(EXTRA_FORMAT_LABEL, request.formatLabel)
                putExtra(EXTRA_IS_VIDEO_ONLY, request.isVideoOnly)
                putExtra(EXTRA_SHARE_ONLY, request.shareOnly)
                putExtra(EXTRA_DIRECT_DOWNLOAD_URL, request.directDownloadUrl)
                request.existingRecordId?.let { putExtra(EXTRA_EXISTING_RECORD_ID, it) }
            }
        ContextCompat.startForegroundService(context, intent)
    }

    override fun cancelDownload(requestId: String) {
        val intent =
            Intent(ACTION_CANCEL_DOWNLOAD).apply {
                component = ComponentName(context.packageName, DOWNLOAD_SERVICE_CLASS)
                putExtra(EXTRA_REQUEST_ID, requestId)
            }
        context.startService(intent)
    }

    /**
     * Update the download state from the existing DownloadServiceStateHolder.
     * Called by the Hilt bridge to sync Android service state with the shared interface.
     */
    fun updateState(newState: DownloadServiceState) {
        _downloadState.value = newState
    }

    companion object {
        // Mirrors DownloadService companion constants (can't import from feature module)
        private const val DOWNLOAD_SERVICE_CLASS =
            "com.socialvideodownloader.feature.download.service.DownloadService"
        private const val ACTION_START_DOWNLOAD =
            "com.socialvideodownloader.action.START_DOWNLOAD"
        private const val ACTION_CANCEL_DOWNLOAD =
            "com.socialvideodownloader.action.CANCEL_DOWNLOAD"
        private const val EXTRA_REQUEST_ID = "extra_request_id"
        private const val EXTRA_SOURCE_URL = "extra_source_url"
        private const val EXTRA_VIDEO_TITLE = "extra_video_title"
        private const val EXTRA_THUMBNAIL_URL = "extra_thumbnail_url"
        private const val EXTRA_FORMAT_ID = "extra_format_id"
        private const val EXTRA_FORMAT_LABEL = "extra_format_label"
        private const val EXTRA_IS_VIDEO_ONLY = "extra_is_video_only"
        private const val EXTRA_SHARE_ONLY = "extra_share_only"
        private const val EXTRA_DIRECT_DOWNLOAD_URL = "extra_direct_download_url"
        private const val EXTRA_EXISTING_RECORD_ID = "extra_existing_record_id"
    }
}
