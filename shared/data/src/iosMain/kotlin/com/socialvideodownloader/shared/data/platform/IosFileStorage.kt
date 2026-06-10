package com.socialvideodownloader.shared.data.platform

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSError
import platform.Foundation.NSFileManager
import platform.Foundation.NSNumber
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.Foundation.lastPathComponent

private const val SVD_DIRECTORY = "SocialVideoDownloader"

/**
 * iOS implementation of [PlatformFileStorage].
 *
 * Files are stored in `Documents/SocialVideoDownloader/` which is visible
 * in the iOS Files app when UIFileSharingEnabled is set in Info.plist.
 *
 * [platformUri] is always null on iOS — sharing is done via file:// URLs.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class IosFileStorage : PlatformFileStorage {
    /**
     * Move a downloaded file from its temp location into the permanent
     * `Documents/SocialVideoDownloader/` directory.
     */
    override suspend fun saveToDownloads(
        tempFilePath: String,
        fileName: String,
        mimeType: String,
    ): SaveResult =
        withContext(Dispatchers.Default) {
            val destDir = svdDirectory() ?: throw StorageException("Cannot resolve Documents directory")
            ensureDirectory(destDir)

            val safeFileName = FileNames.sanitize(fileName)
            val destUrl =
                destDir.URLByAppendingPathComponent(safeFileName)
                    ?: throw StorageException("Cannot build destination URL")
            val uniqueDestUrl = uniqueUrl(destUrl)

            val sourceUrl = NSURL.fileURLWithPath(tempFilePath)
            moveOrCopy(sourceUrl, uniqueDestUrl)

            val filePath = uniqueDestUrl.path ?: throw StorageException("Destination path is nil")
            val fileSize = fileSizeOf(filePath)

            // iOS uses file:// URLs for sharing, not content:// URIs
            SaveResult(
                filePath = filePath,
                platformUri = null,
                fileSizeBytes = fileSize,
            )
        }

    override suspend fun isFileAccessible(filePath: String): Boolean =
        withContext(Dispatchers.Default) {
            NSFileManager.defaultManager.fileExistsAtPath(filePath)
        }

    override suspend fun deleteFile(filePath: String): Boolean =
        withContext(Dispatchers.Default) {
            val fileManager = NSFileManager.defaultManager
            if (!fileManager.fileExistsAtPath(filePath)) return@withContext true
            fileManager.removeItemAtPath(filePath, error = null)
        }

    /**
     * Returns a `file://` URL string suitable for sharing via UIActivityViewController.
     */
    override suspend fun getShareableUri(filePath: String): String? =
        withContext(Dispatchers.Default) {
            if (!NSFileManager.defaultManager.fileExistsAtPath(filePath)) {
                return@withContext null
            }
            NSURL.fileURLWithPath(filePath).absoluteString
        }

    // --- Helpers ---

    private fun svdDirectory(): NSURL? {
        val docDir =
            NSFileManager.defaultManager.URLForDirectory(
                directory = NSDocumentDirectory,
                inDomain = NSUserDomainMask,
                appropriateForURL = null,
                create = true,
                error = null,
            ) ?: return null
        return docDir.URLByAppendingPathComponent(SVD_DIRECTORY)
    }

    private fun ensureDirectory(url: NSURL) {
        val path = url.path ?: return
        if (!NSFileManager.defaultManager.fileExistsAtPath(path)) {
            NSFileManager.defaultManager.createDirectoryAtPath(
                path = path,
                withIntermediateDirectories = true,
                attributes = null,
                error = null,
            )
        }
    }

    /**
     * If the destination already exists, append a numeric suffix before the extension.
     * e.g. "video.mp4" → "video (1).mp4"
     */
    private fun uniqueUrl(base: NSURL): NSURL {
        val fileManager = NSFileManager.defaultManager
        if (base.path?.let { fileManager.fileExistsAtPath(it) } != true) return base

        val dir = base.URLByDeletingLastPathComponent ?: base
        val name = base.lastPathComponent ?: "file"
        val dotIdx = name.lastIndexOf('.')
        val baseName = if (dotIdx >= 0) name.substring(0, dotIdx) else name
        val ext = if (dotIdx >= 0) name.substring(dotIdx) else ""

        var counter = 1
        while (true) {
            val candidate = dir.URLByAppendingPathComponent("$baseName ($counter)$ext") ?: break
            if (candidate.path?.let { fileManager.fileExistsAtPath(it) } != true) return candidate
            counter++
        }
        return base
    }

    /**
     * Moves [source] to [dest], falling back to copy + delete if a plain move fails
     * (e.g. across volumes). Surfaces the underlying [NSError] description on failure
     * instead of swallowing it.
     */
    private fun moveOrCopy(
        source: NSURL,
        dest: NSURL,
    ) {
        val fileManager = NSFileManager.defaultManager
        memScoped {
            val moveError = alloc<ObjCObjectVar<NSError?>>()
            if (fileManager.moveItemAtURL(source, toURL = dest, error = moveError.ptr)) return

            val copyError = alloc<ObjCObjectVar<NSError?>>()
            if (!fileManager.copyItemAtURL(source, toURL = dest, error = copyError.ptr)) {
                throw StorageException(
                    "Failed to save file to Downloads directory " +
                        "(move: ${moveError.value?.localizedDescription ?: "unknown"}; " +
                        "copy: ${copyError.value?.localizedDescription ?: "unknown"})",
                )
            }
            fileManager.removeItemAtURL(source, error = null)
        }
    }

    /** Reads the file size, correctly bridging the `NSFileSize` [NSNumber] to a [Long]. */
    private fun fileSizeOf(path: String): Long =
        (NSFileManager.defaultManager.attributesOfItemAtPath(path, error = null)?.get("NSFileSize") as? NSNumber)
            ?.longValue ?: 0L
}

class StorageException(message: String) : Exception(message)
