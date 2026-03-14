package com.socialvideodownloader.core.data.local

import com.socialvideodownloader.core.domain.model.DownloadRecord
import com.socialvideodownloader.core.domain.model.DownloadStatus

fun DownloadEntity.toDomain(): DownloadRecord = DownloadRecord(
    id = id,
    sourceUrl = sourceUrl,
    videoTitle = videoTitle,
    thumbnailUrl = thumbnailUrl,
    filePath = filePath,
    status = DownloadStatus.valueOf(status),
    createdAt = createdAt,
    completedAt = completedAt,
    fileSizeBytes = fileSizeBytes,
)

fun DownloadRecord.toEntity(): DownloadEntity = DownloadEntity(
    id = id,
    sourceUrl = sourceUrl,
    videoTitle = videoTitle,
    thumbnailUrl = thumbnailUrl,
    filePath = filePath,
    status = status.name,
    createdAt = createdAt,
    completedAt = completedAt,
    fileSizeBytes = fileSizeBytes,
)
