package com.socialvideodownloader.core.data.file

import android.content.Context
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.socialvideodownloader.core.domain.di.IoDispatcher
import com.socialvideodownloader.core.domain.file.FileAccessManager
import com.socialvideodownloader.core.domain.model.DownloadRecord
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class AndroidFileAccessManager @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : FileAccessManager {

    override suspend fun resolveContentUri(record: DownloadRecord): String? =
        withContext(ioDispatcher) {
            if (record.mediaStoreUri != null) {
                return@withContext record.mediaStoreUri
            }
            val filePath = record.filePath ?: return@withContext null
            try {
                val file = File(filePath)
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file,
                )
                uri.toString()
            } catch (_: Exception) {
                null
            }
        }

    override suspend fun isFileAccessible(contentUri: String): Boolean =
        withContext(ioDispatcher) {
            try {
                val uri = contentUri.toUri()
                context.contentResolver.openInputStream(uri)?.use { true } ?: false
            } catch (_: Exception) {
                false
            }
        }

    override suspend fun deleteFile(contentUri: String): Boolean =
        withContext(ioDispatcher) {
            try {
                val uri = contentUri.toUri()
                when (uri.scheme) {
                    "content" -> context.contentResolver.delete(uri, null, null) > 0
                    "file" -> File(uri.path ?: return@withContext false).delete()
                    else -> false
                }
            } catch (_: Exception) {
                false
            }
        }
}
