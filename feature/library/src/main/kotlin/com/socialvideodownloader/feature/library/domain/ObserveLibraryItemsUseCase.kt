package com.socialvideodownloader.feature.library.domain

import com.socialvideodownloader.core.domain.file.FileAccessManager
import com.socialvideodownloader.core.domain.model.LibraryItem
import com.socialvideodownloader.core.domain.repository.DownloadRepository
import com.socialvideodownloader.core.domain.util.PlatformNameResolver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ObserveLibraryItemsUseCase @Inject constructor(
    private val downloadRepository: DownloadRepository,
    private val fileManager: FileAccessManager,
) {
    operator fun invoke(): Flow<List<LibraryItem>> =
        downloadRepository.getCompletedDownloads().map { records ->
            records.mapNotNull { record ->
                val contentUri = fileManager.resolveContentUri(record) ?: return@mapNotNull null
                if (!fileManager.isFileAccessible(contentUri)) return@mapNotNull null
                LibraryItem(
                    id = record.id,
                    title = record.videoTitle,
                    formatLabel = record.formatLabel.ifBlank { null },
                    thumbnailUrl = record.thumbnailUrl,
                    platformName = PlatformNameResolver.nameFromUrl(record.sourceUrl) ?: "",
                    completedAt = record.completedAt ?: record.createdAt,
                    fileSizeBytes = record.fileSizeBytes,
                    contentUri = contentUri,
                )
            }
        }
}
