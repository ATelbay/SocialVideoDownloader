package com.videograb.core.data.local

import com.videograb.core.domain.model.DownloadRecord
import com.videograb.core.domain.model.DownloadStatus

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
