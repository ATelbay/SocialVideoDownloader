package com.socialvideodownloader.core.domain.model

data class DownloadProgress(
    val requestId: String,
    val progressPercent: Float,
    val downloadedBytes: Long,
    val totalBytes: Long? = null,
    val speedBytesPerSec: Long,
    val etaSeconds: Long,
    val isMuxing: Boolean = false,
)
