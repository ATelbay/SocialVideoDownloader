package com.videograb.core.domain.model

data class DownloadRecord(
    val id: Long = 0,
    val sourceUrl: String,
    val videoTitle: String,
    val thumbnailUrl: String? = null,
    val filePath: String? = null,
    val status: DownloadStatus,
    val createdAt: Long,
    val completedAt: Long? = null,
    val fileSizeBytes: Long? = null,
)
