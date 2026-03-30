package com.socialvideodownloader.shared.feature.library

sealed interface LibraryUiState {
    data object Loading : LibraryUiState
    data object Empty : LibraryUiState
    data class Content(val items: List<LibraryListItem>) : LibraryUiState
}

data class LibraryListItem(
    val id: Long,
    val title: String,
    val formatLabel: String?,
    val thumbnailUrl: String?,
    val platformName: String,
    val completedAt: Long,
    val fileSizeBytes: Long?,
    val contentUri: String,
)
