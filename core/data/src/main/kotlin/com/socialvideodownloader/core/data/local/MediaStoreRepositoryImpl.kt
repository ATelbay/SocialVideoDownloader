package com.socialvideodownloader.core.data.local

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import com.socialvideodownloader.core.domain.di.IoDispatcher
import com.socialvideodownloader.core.domain.repository.MediaStoreRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import javax.inject.Inject

class MediaStoreRepositoryImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : MediaStoreRepository {
        override suspend fun saveToDownloads(
            tempFilePath: String,
            title: String,
            mimeType: String,
        ): String =
            withContext(ioDispatcher) {
                val tempFile = File(tempFilePath)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    saveWithMediaStore(tempFile, title, mimeType)
                } else {
                    saveDirectly(tempFile, title)
                }
            }

        private fun sanitizeFileName(name: String): String {
            val sanitized =
                name.replace(Regex("[/\\\\:*?\"<>|]"), "_")
                    .trimStart('.')
                    .take(200)
                    .ifBlank { "download" }
            return sanitized
        }

        @RequiresApi(Build.VERSION_CODES.Q)
        private fun saveWithMediaStore(
            tempFile: File,
            title: String,
            mimeType: String,
        ): String {
            val safeTitle = sanitizeFileName(title)
            val contentValues =
                ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, safeTitle)
                    put(MediaStore.Downloads.MIME_TYPE, mimeType)
                    put(MediaStore.Downloads.RELATIVE_PATH, "Download/SocialVideoDownloader")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
            val resolver = context.contentResolver
            val uri =
                resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    ?: throw IllegalStateException("Failed to create MediaStore entry")

            try {
                val outputStream =
                    resolver.openOutputStream(uri)
                        ?: run {
                            resolver.delete(uri, null, null)
                            throw IOException("Failed to open output stream for MediaStore entry")
                        }
                outputStream.use { os ->
                    FileInputStream(tempFile).use { inputStream ->
                        inputStream.copyTo(os)
                    }
                }
                contentValues.clear()
                contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            } catch (e: Exception) {
                resolver.delete(uri, null, null)
                throw e
            } finally {
                tempFile.delete()
            }

            return uri.toString()
        }

        @Suppress("DEPRECATION")
        private fun saveDirectly(
            tempFile: File,
            title: String,
        ): String {
            val downloadsDir =
                File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "SocialVideoDownloader",
                )
            downloadsDir.mkdirs()
            val safeTitle = sanitizeFileName(title)
            val destFile = File(downloadsDir, safeTitle)
            require(destFile.canonicalPath.startsWith(downloadsDir.canonicalPath)) {
                "Path traversal detected"
            }
            tempFile.copyTo(destFile, overwrite = true)
            tempFile.delete()
            return destFile.absolutePath
        }
    }
