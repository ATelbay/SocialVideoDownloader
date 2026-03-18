package com.socialvideodownloader.core.domain.model

data class LibraryItem(
    val id: Long,
    val title: String,
    val formatLabel: String?,
    val thumbnailUrl: String?,
    val platformName: String,
    val completedAt: Long,
    val fileSizeBytes: Long?,
    val contentUri: String,
)
