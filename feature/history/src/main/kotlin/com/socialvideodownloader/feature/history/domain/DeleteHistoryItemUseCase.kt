package com.socialvideodownloader.feature.history.domain

import com.socialvideodownloader.core.domain.repository.DownloadRepository
import com.socialvideodownloader.feature.history.file.HistoryFileManager
import javax.inject.Inject

class DeleteHistoryItemUseCase @Inject constructor(
    private val repository: DownloadRepository,
    private val fileManager: HistoryFileManager,
) {
    suspend operator fun invoke(itemId: Long, deleteFile: Boolean): DeleteResult {
        val record = repository.getById(itemId) ?: return DeleteResult(fileDeleteFailed = false)

        repository.delete(record)

        if (deleteFile) {
            val uri = fileManager.resolveContentUri(record)
            if (uri != null) {
                val deleted = runCatching { fileManager.deleteFile(uri) }.getOrDefault(false)
                if (!deleted) return DeleteResult(fileDeleteFailed = true)
            }
        }

        return DeleteResult(fileDeleteFailed = false)
    }
}

data class DeleteResult(val fileDeleteFailed: Boolean)
