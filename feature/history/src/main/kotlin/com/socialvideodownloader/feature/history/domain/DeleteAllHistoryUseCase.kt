package com.socialvideodownloader.feature.history.domain

import com.socialvideodownloader.core.domain.repository.DownloadRepository
import com.socialvideodownloader.feature.history.file.HistoryFileManager
import javax.inject.Inject
import kotlinx.coroutines.flow.first

data class DeleteAllHistoryResult(
    val failedFileDeletions: Int,
)

class DeleteAllHistoryUseCase @Inject constructor(
    private val repository: DownloadRepository,
    private val fileManager: HistoryFileManager,
) {
    suspend operator fun invoke(): DeleteAllHistoryResult {
        val records = repository.getAll().first()

        var failedFileDeletions = 0
        for (record in records) {
            val uri = fileManager.resolveContentUri(record) ?: continue
            val deleted = runCatching { fileManager.deleteFile(uri) }.getOrDefault(false)
            if (!deleted) failedFileDeletions++
        }

        repository.deleteAll()

        return DeleteAllHistoryResult(failedFileDeletions = failedFileDeletions)
    }
}
