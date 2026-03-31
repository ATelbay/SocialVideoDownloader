package com.socialvideodownloader.feature.history.domain

import com.socialvideodownloader.core.domain.file.FileAccessManager
import com.socialvideodownloader.core.domain.repository.DownloadRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

data class DeleteAllHistoryResult(
    val failedFileDeletions: Int,
)

class DeleteAllHistoryUseCase
    @Inject
    constructor(
        private val repository: DownloadRepository,
        private val fileManager: FileAccessManager,
    ) {
        suspend operator fun invoke(deleteFiles: Boolean = true): DeleteAllHistoryResult {
            var failedFileDeletions = 0

            if (deleteFiles) {
                val records = repository.getAll().first()
                for (record in records) {
                    val uri = fileManager.resolveContentUri(record) ?: continue
                    val deleted = runCatching { fileManager.deleteFile(uri) }.getOrDefault(false)
                    if (!deleted) failedFileDeletions++
                }
            }

            repository.deleteAll()

            return DeleteAllHistoryResult(failedFileDeletions = failedFileDeletions)
        }
    }
