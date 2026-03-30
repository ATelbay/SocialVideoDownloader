package com.socialvideodownloader.shared.feature.download

import com.socialvideodownloader.core.domain.model.DownloadProgress
import com.socialvideodownloader.core.domain.model.ExistingDownload
import com.socialvideodownloader.core.domain.model.VideoMetadata
import com.socialvideodownloader.shared.data.platform.DownloadErrorType

sealed interface DownloadUiState {
    data class Idle(
        val existingDownload: ExistingDownload? = null,
        val prefillUrl: String? = null,
    ) : DownloadUiState

    data class Extracting(val url: String) : DownloadUiState

    data class FormatSelection(
        val metadata: VideoMetadata,
        val selectedFormatId: String,
    ) : DownloadUiState

    data class Downloading(
        val metadata: VideoMetadata,
        val progress: DownloadProgress,
        val selectedFormatId: String,
        val isShareMode: Boolean = false,
    ) : DownloadUiState

    data class Done(
        val metadata: VideoMetadata,
        val filePath: String,
        val fileUri: String? = null,
    ) : DownloadUiState

    data class Error(
        val errorType: DownloadErrorType,
        val message: String?,
        val retryAction: RetryAction?,
    ) : DownloadUiState
}

sealed interface RetryAction {
    data class RetryExtraction(val url: String) : RetryAction
}
