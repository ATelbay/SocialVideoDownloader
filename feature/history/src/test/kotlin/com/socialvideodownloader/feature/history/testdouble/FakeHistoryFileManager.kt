package com.socialvideodownloader.feature.history.testdouble

import com.socialvideodownloader.core.domain.file.FileAccessManager
import com.socialvideodownloader.core.domain.model.DownloadRecord

class FakeHistoryFileManager : FileAccessManager {

    var resolveContentUriResult: (DownloadRecord) -> String? = { null }
    var isFileAccessibleResult: (String) -> Boolean = { false }
    var deleteFileResult: (String) -> Boolean = { false }

    override suspend fun resolveContentUri(record: DownloadRecord): String? =
        resolveContentUriResult(record)

    override suspend fun isFileAccessible(contentUri: String): Boolean =
        isFileAccessibleResult(contentUri)

    override suspend fun deleteFile(contentUri: String): Boolean =
        deleteFileResult(contentUri)
}
