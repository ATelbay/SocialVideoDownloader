package com.socialvideodownloader.core.domain.model

data class HistoryItem(
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
