package com.socialvideodownloader.core.domain.model

enum class DownloadStatus {
    PENDING,
    QUEUED,
    DOWNLOADING,
    COMPLETED,
    FAILED,
    CANCELLED,
}
