package com.socialvideodownloader.core.domain.model

data class DownloadRequest(
    val id: String,
    val sourceUrl: String,
    val videoTitle: String,
    val thumbnailUrl: String? = null,
    val formatId: String,
    val ext: String = "mp4",
    val formatLabel: String,
    val isVideoOnly: Boolean,
    val totalBytes: Long? = null,
    val shareOnly: Boolean = false,
    val existingRecordId: Long? = null,
    val directDownloadUrl: String? = null,
)
