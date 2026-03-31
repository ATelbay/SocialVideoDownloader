package com.socialvideodownloader.feature.history.domain

import com.socialvideodownloader.core.domain.file.FileAccessManager
import com.socialvideodownloader.core.domain.model.HistoryItem
import com.socialvideodownloader.core.domain.repository.DownloadRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ObserveHistoryItemsUseCase
    @Inject
    constructor(
        private val downloadRepository: DownloadRepository,
        private val fileManager: FileAccessManager,
    ) {
        operator fun invoke(): Flow<List<HistoryItem>> =
            downloadRepository.getAll().map { records ->
                records.map { record ->
                    val contentUri = fileManager.resolveContentUri(record)
                    val isAccessible = contentUri?.let { fileManager.isFileAccessible(it) } ?: false
                    HistoryItem(
                        id = record.id,
                        title = record.videoTitle,
                        formatLabel = record.formatLabel,
                        thumbnailUrl = record.thumbnailUrl,
                        sourceUrl = record.sourceUrl,
                        status = record.status,
                        createdAt = record.createdAt,
                        fileSizeBytes = record.fileSizeBytes,
                        contentUri = contentUri,
                        isFileAccessible = isAccessible,
                    )
                }
            }
    }
