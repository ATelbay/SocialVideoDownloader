package com.socialvideodownloader.feature.download.ui

import com.socialvideodownloader.core.domain.model.DownloadProgress
import com.socialvideodownloader.core.domain.model.VideoMetadata

sealed interface DownloadUiState {
    data object Idle : DownloadUiState
    data class Extracting(val url: String) : DownloadUiState
    data class FormatSelection(
        val metadata: VideoMetadata,
        val selectedFormatId: String,
    ) : DownloadUiState
    data class Downloading(
        val metadata: VideoMetadata,
        val progress: DownloadProgress,
        val selectedFormatId: String,
    ) : DownloadUiState
    data class Done(
        val metadata: VideoMetadata,
        val filePath: String,
    ) : DownloadUiState
    data class Error(
        val message: String,
        val retryAction: RetryAction,
    ) : DownloadUiState
}

sealed interface RetryAction {
    data class RetryExtraction(val url: String) : RetryAction
}
