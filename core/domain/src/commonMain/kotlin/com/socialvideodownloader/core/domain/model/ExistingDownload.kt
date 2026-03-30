package com.socialvideodownloader.core.domain.model

data class ExistingDownload(
    val recordId: Long,
    val videoTitle: String,
    val formatLabel: String,
    val thumbnailUrl: String?,
    val contentUri: String,
    val completedAt: Long,
    val fileSizeBytes: Long?,
)
