package com.socialvideodownloader.feature.history.file

import com.socialvideodownloader.core.domain.model.DownloadRecord

interface HistoryFileManager {
    suspend fun resolveContentUri(record: DownloadRecord): String?
    suspend fun isFileAccessible(contentUri: String): Boolean
    suspend fun deleteFile(contentUri: String): Boolean
}
