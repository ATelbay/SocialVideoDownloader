package com.socialvideodownloader.core.domain.usecase

import com.socialvideodownloader.core.domain.file.FileAccessManager
import com.socialvideodownloader.core.domain.model.ExistingDownload
import com.socialvideodownloader.core.domain.repository.DownloadRepository
import com.socialvideodownloader.core.domain.util.UrlNormalizer
import javax.inject.Inject

class FindExistingDownloadUseCase
    @Inject
    constructor(
        private val downloadRepository: DownloadRepository,
        private val fileAccessManager: FileAccessManager,
    ) {
        suspend operator fun invoke(rawUrl: String): ExistingDownload? {
            val normalizedInput = UrlNormalizer.normalize(rawUrl)

            val completed = downloadRepository.getCompletedSnapshot()
            val match =
                completed.firstOrNull { record ->
                    UrlNormalizer.normalize(record.sourceUrl) == normalizedInput
                } ?: return null

            val contentUri = fileAccessManager.resolveContentUri(match) ?: return null
            val accessible = fileAccessManager.isFileAccessible(contentUri)
            if (!accessible) return null

            return ExistingDownload(
                recordId = match.id,
                videoTitle = match.videoTitle,
                formatLabel = match.formatLabel,
                thumbnailUrl = match.thumbnailUrl,
                contentUri = contentUri,
                completedAt = match.completedAt ?: 0L,
                fileSizeBytes = match.fileSizeBytes,
            )
        }
    }
