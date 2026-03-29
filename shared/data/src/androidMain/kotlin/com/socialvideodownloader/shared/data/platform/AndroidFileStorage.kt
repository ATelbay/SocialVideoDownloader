package com.socialvideodownloader.shared.data.platform

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Android implementation of [PlatformFileStorage].
 *
 * Uses MediaStore API for scoped storage access on Android 10+.
 * Files are saved to Downloads/SocialVideoDownloader/.
 */
class AndroidFileStorage(
    private val context: Context,
) : PlatformFileStorage {

    override suspend fun saveToDownloads(
        tempFilePath: String,
        fileName: String,
        mimeType: String,
    ): SaveResult = withContext(Dispatchers.IO) {
        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, mimeType)
            put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/SocialVideoDownloader")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            ?: throw IllegalStateException("Failed to create MediaStore entry for $fileName")

        val tempFile = File(tempFilePath)
        resolver.openOutputStream(uri)?.use { outputStream ->
            tempFile.inputStream().use { inputStream ->
                inputStream.copyTo(outputStream)
            }
        } ?: throw IllegalStateException("Failed to open output stream for $uri")

        contentValues.clear()
        contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
        resolver.update(uri, contentValues, null, null)

        // Clean up temp file
        tempFile.delete()

        SaveResult(
            filePath = tempFilePath,
            platformUri = uri.toString(),
            fileSizeBytes = tempFile.length(),
        )
    }

    override suspend fun isFileAccessible(filePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // Try as content URI first
            if (filePath.startsWith("content://")) {
                val uri = Uri.parse(filePath)
                context.contentResolver.openFileDescriptor(uri, "r")?.use { true } ?: false
            } else {
                File(filePath).exists()
            }
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun deleteFile(filePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            if (filePath.startsWith("content://")) {
                val uri = Uri.parse(filePath)
                context.contentResolver.delete(uri, null, null) > 0
            } else {
                val file = File(filePath)
                !file.exists() || file.delete()
            }
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun getShareableUri(filePath: String): String? {
        // For content:// URIs, they are already shareable
        if (filePath.startsWith("content://")) {
            return filePath
        }
        // For file paths, we would need FileProvider — handled by the Android UI layer
        return null
    }
}
