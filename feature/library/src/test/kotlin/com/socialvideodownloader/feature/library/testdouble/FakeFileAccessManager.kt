// TODO: Extract to shared test fixtures module (duplicated in feature/history as FakeHistoryFileManager)
package com.socialvideodownloader.feature.library.testdouble

import com.socialvideodownloader.core.domain.file.FileAccessManager
import com.socialvideodownloader.core.domain.model.DownloadRecord

class FakeFileAccessManager : FileAccessManager {
    var resolveContentUriResult: (DownloadRecord) -> String? = { null }
    var isFileAccessibleResult: (String) -> Boolean = { false }
    var deleteFileResult: (String) -> Boolean = { false }

    override suspend fun resolveContentUri(record: DownloadRecord): String? = resolveContentUriResult(record)

    override suspend fun isFileAccessible(contentUri: String): Boolean = isFileAccessibleResult(contentUri)

    override suspend fun deleteFile(contentUri: String): Boolean = deleteFileResult(contentUri)
}
