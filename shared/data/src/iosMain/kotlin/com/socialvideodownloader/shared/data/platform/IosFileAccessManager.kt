package com.socialvideodownloader.shared.data.platform

import com.socialvideodownloader.core.domain.file.FileAccessManager
import com.socialvideodownloader.core.domain.model.DownloadRecord
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL

@OptIn(ExperimentalForeignApi::class)
class IosFileAccessManager : FileAccessManager {

    override suspend fun resolveContentUri(record: DownloadRecord): String? {
        return record.mediaStoreUri ?: record.filePath
    }

    override suspend fun isFileAccessible(contentUri: String): Boolean {
        val path = resolvePath(contentUri) ?: return false
        return NSFileManager.defaultManager.fileExistsAtPath(path)
    }

    override suspend fun deleteFile(contentUri: String): Boolean {
        val path = resolvePath(contentUri) ?: return false
        val fileManager = NSFileManager.defaultManager
        if (!fileManager.fileExistsAtPath(path)) return true
        return fileManager.removeItemAtPath(path, error = null)
    }

    private fun resolvePath(contentUri: String): String? {
        return if (contentUri.startsWith("file://")) {
            NSURL(string = contentUri)?.path
        } else {
            contentUri
        }
    }
}
