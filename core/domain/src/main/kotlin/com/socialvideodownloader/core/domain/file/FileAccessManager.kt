package com.socialvideodownloader.core.domain.file

import com.socialvideodownloader.core.domain.model.DownloadRecord

interface FileAccessManager {
    suspend fun resolveContentUri(record: DownloadRecord): String?

    suspend fun isFileAccessible(contentUri: String): Boolean

    suspend fun deleteFile(contentUri: String): Boolean
}
