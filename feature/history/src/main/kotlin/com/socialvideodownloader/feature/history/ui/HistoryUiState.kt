package com.socialvideodownloader.feature.history.ui

import com.socialvideodownloader.core.domain.model.DownloadStatus

data class HistoryListItem(
    val id: Long,
    val title: String,
    val formatLabel: String?,
    val thumbnailUrl: String?,
    val status: DownloadStatus,
    val createdAt: Long,
    val fileSizeBytes: Long?,
    val contentUri: String?,
    val isFileAccessible: Boolean,
)

sealed interface DeleteTarget {
    data class Single(val itemId: Long) : DeleteTarget
    data object All : DeleteTarget
}

data class DeleteConfirmationState(
    val target: DeleteTarget,
    val hasAnyAccessibleFile: Boolean,
    val deleteFilesSelected: Boolean = false,
    val affectedCount: Int,
)

sealed interface HistoryUiState {
    data object Loading : HistoryUiState
    data class Empty(val query: String, val isFiltering: Boolean) : HistoryUiState
    data class Content(
        val query: String,
        val items: List<HistoryListItem>,
        val openMenuItemId: Long? = null,
        val deleteConfirmation: DeleteConfirmationState? = null,
        val isDeleting: Boolean = false,
    ) : HistoryUiState
}
