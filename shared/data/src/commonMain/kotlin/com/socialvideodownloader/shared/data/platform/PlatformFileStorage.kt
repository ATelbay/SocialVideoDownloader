package com.socialvideodownloader.shared.data.platform

/**
 * Platform abstraction for file storage operations.
 *
 * Android: Wraps MediaStore + ContentResolver.
 * iOS: Uses Documents directory + FileManager.
 */
interface PlatformFileStorage {

    /**
     * Move a downloaded file from temp location to the platform's downloads directory.
     * Returns the permanent file path and optional platform URI.
     */
    suspend fun saveToDownloads(tempFilePath: String, fileName: String, mimeType: String): SaveResult

    /** Check if a previously saved file still exists and is accessible. */
    suspend fun isFileAccessible(filePath: String): Boolean

    /** Delete a downloaded file. Returns true if deleted or already absent. */
    suspend fun deleteFile(filePath: String): Boolean

    /** Get a URI suitable for sharing the file with other apps. */
    suspend fun getShareableUri(filePath: String): String?
}

/** Result of saving a file to the downloads directory. */
data class SaveResult(
    val filePath: String,
    val platformUri: String?,
    val fileSizeBytes: Long,
)
