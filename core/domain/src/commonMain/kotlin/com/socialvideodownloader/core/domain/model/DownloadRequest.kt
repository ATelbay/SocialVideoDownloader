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
    /**
     * Direct URL of a mux-compatible audio stream. Set only when [isVideoOnly] is true and the
     * download uses [directDownloadUrl] (server extraction path), so the platform downloader can
     * fetch both streams and merge them on-device.
     */
    val audioDirectUrl: String? = null,
)
