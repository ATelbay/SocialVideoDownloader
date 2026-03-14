package com.socialvideodownloader.core.data.local

import com.socialvideodownloader.core.domain.model.DownloadRecord
import com.socialvideodownloader.core.domain.model.DownloadStatus

fun DownloadEntity.toDomain(): DownloadRecord = DownloadRecord(
    id = id,
    sourceUrl = sourceUrl,
    videoTitle = videoTitle,
    thumbnailUrl = thumbnailUrl,
    formatLabel = formatLabel,
    filePath = filePath,
    status = DownloadStatus.entries.find { it.name == status } ?: DownloadStatus.FAILED,
    createdAt = createdAt,
    completedAt = completedAt,
    fileSizeBytes = fileSizeBytes,
)

fun DownloadRecord.toEntity(): DownloadEntity = DownloadEntity(
    id = id,
    sourceUrl = sourceUrl,
    videoTitle = videoTitle,
    thumbnailUrl = thumbnailUrl,
    formatLabel = formatLabel,
    filePath = filePath,
    status = status.name,
    createdAt = createdAt,
    completedAt = completedAt,
    fileSizeBytes = fileSizeBytes,
)
