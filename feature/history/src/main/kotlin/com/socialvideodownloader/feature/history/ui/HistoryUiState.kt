package com.socialvideodownloader.feature.history.ui

import com.socialvideodownloader.core.domain.model.DownloadStatus
import com.socialvideodownloader.core.domain.model.SyncStatus
import com.socialvideodownloader.core.domain.sync.CloudCapacity

data class HistoryListItem(
    val id: Long,
    val title: String,
    val formatLabel: String?,
    val thumbnailUrl: String?,
    val sourceUrl: String,
    val status: DownloadStatus,
    val createdAt: Long,
    val fileSizeBytes: Long?,
    val contentUri: String?,
    val isFileAccessible: Boolean,
)

sealed interface DeleteTarget {
    data class Single(val itemId: Long) : DeleteTarget
}

data class DeleteConfirmationState(
    val target: DeleteTarget,
    val hasAnyAccessibleFile: Boolean,
    val deleteFilesSelected: Boolean = false,
    val affectedCount: Int,
)

data class CloudBackupState(
    val isCloudBackupEnabled: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus.Idle,
    val restoreState: RestoreState = RestoreState.Idle,
)

sealed interface RestoreState {
    data object Idle : RestoreState
    data class InProgress(val current: Int, val total: Int) : RestoreState
    data class Completed(val restored: Int, val skipped: Int) : RestoreState
    data class Error(val message: String) : RestoreState
}

sealed interface HistoryUiState {
    data object Loading : HistoryUiState
    data class Empty(val query: String, val isFiltering: Boolean) : HistoryUiState
    data class Content(
        val query: String,
        val items: List<HistoryListItem>,
        val openMenuItemId: Long? = null,
        val deleteConfirmation: DeleteConfirmationState? = null,
        val isDeleting: Boolean = false,
        // US3: Billing — capacity info for banner
        val cloudCapacity: CloudCapacity? = null,
    ) : HistoryUiState
}
