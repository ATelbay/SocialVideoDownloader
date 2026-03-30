package com.socialvideodownloader.shared.feature.history

import com.socialvideodownloader.core.domain.file.FileAccessManager
import com.socialvideodownloader.core.domain.repository.DownloadRepository

/**
 * KMP-compatible version of the history delete use case.
 *
 * Mirrors the logic from [com.socialvideodownloader.feature.history.domain.DeleteHistoryItemUseCase]
 * but removes the Hilt @Inject annotation so it can live in commonMain.
 */
class DeleteHistoryItemUseCaseShared(
    private val repository: DownloadRepository,
    private val fileManager: FileAccessManager,
) {
    suspend operator fun invoke(itemId: Long, deleteFile: Boolean): DeleteResult {
        val record = repository.getById(itemId) ?: return DeleteResult(fileDeleteFailed = false)

        var fileDeleteFailed = false
        if (deleteFile) {
            val uri = fileManager.resolveContentUri(record)
            if (uri != null) {
                val deleted = runCatching { fileManager.deleteFile(uri) }.getOrDefault(false)
                if (!deleted) fileDeleteFailed = true
            }
        }

        repository.delete(record)

        return DeleteResult(fileDeleteFailed = fileDeleteFailed)
    }
}

data class DeleteResult(val fileDeleteFailed: Boolean)
