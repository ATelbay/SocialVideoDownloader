package com.socialvideodownloader.feature.history.domain

import com.socialvideodownloader.core.domain.file.FileAccessManager
import com.socialvideodownloader.core.domain.repository.DownloadRepository
import javax.inject.Inject

class DeleteHistoryItemUseCase @Inject constructor(
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
