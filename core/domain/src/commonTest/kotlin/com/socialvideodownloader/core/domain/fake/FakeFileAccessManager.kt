package com.socialvideodownloader.core.domain.fake

import com.socialvideodownloader.core.domain.file.FileAccessManager
import com.socialvideodownloader.core.domain.model.DownloadRecord

class FakeFileAccessManager : FileAccessManager {
    var contentUriMap = mutableMapOf<Long, String?>()
    var accessibleUris = mutableSetOf<String>()

    override suspend fun resolveContentUri(record: DownloadRecord): String? =
        contentUriMap[record.id]

    override suspend fun isFileAccessible(contentUri: String): Boolean =
        contentUri in accessibleUris

    override suspend fun deleteFile(contentUri: String): Boolean {
        accessibleUris.remove(contentUri)
        return true
    }
}
