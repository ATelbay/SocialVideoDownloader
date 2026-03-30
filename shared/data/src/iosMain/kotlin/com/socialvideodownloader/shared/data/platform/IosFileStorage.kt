package com.socialvideodownloader.shared.data.platform

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSError
import platform.Foundation.NSFileManager
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
@OptIn(ExperimentalForeignApi::class)
class IosFileStorage : PlatformFileStorage {
    /**
     * Move a downloaded file from its temp location into the permanent
     * `Documents/SocialVideoDownloader/` directory.
     */
    override suspend fun saveToDownloads(
        tempFilePath: String,
        fileName: String,
        mimeType: String,
    ): SaveResult {
        val destDir = svdDirectory() ?: throw StorageException("Cannot resolve Documents directory")
        ensureDirectory(destDir)

        val safeFileName = sanitizeFileName(fileName)
        val destUrl = destDir.URLByAppendingPathComponent(safeFileName) ?: throw StorageException("Cannot build destination URL")
        val uniqueDestUrl = uniqueUrl(destUrl)

        val sourceUrl = NSURL.fileURLWithPath(tempFilePath)
        val fileManager = NSFileManager.defaultManager

        val error: NSError? = null

        @Suppress("UNCHECKED_CAST")
        val moved = fileManager.moveItemAtURL(sourceUrl, toURL = uniqueDestUrl, error = null)
        if (!moved) {
            // If move failed (e.g., cross-device), try copy + delete
            val copied = fileManager.copyItemAtURL(sourceUrl, toURL = uniqueDestUrl, error = null)
            if (!copied) {
                throw StorageException("Failed to save file to Downloads directory")
            }
            fileManager.removeItemAtURL(sourceUrl, error = null)
        }

        val filePath = uniqueDestUrl.path ?: throw StorageException("Destination path is nil")
        val fileSize =
            fileManager.attributesOfItemAtPath(filePath, error = null)
                ?.get("NSFileSize") as? Long ?: 0L

        // iOS uses file:// URLs for sharing, not content:// URIs
        return SaveResult(
            filePath = filePath,
            platformUri = null,
            fileSizeBytes = fileSize,
        )
    }

    override suspend fun isFileAccessible(filePath: String): Boolean {
        return NSFileManager.defaultManager.fileExistsAtPath(filePath)
    }

    override suspend fun deleteFile(filePath: String): Boolean {
        val fileManager = NSFileManager.defaultManager
        if (!fileManager.fileExistsAtPath(filePath)) return true
        return fileManager.removeItemAtPath(filePath, error = null)
    }

    /**
     * Returns a `file://` URL string suitable for sharing via UIActivityViewController.
     */
    override suspend fun getShareableUri(filePath: String): String? {
        if (!NSFileManager.defaultManager.fileExistsAtPath(filePath)) return null
        return NSURL.fileURLWithPath(filePath).absoluteString
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

    private fun sanitizeFileName(name: String): String {
        val cleaned = name.replace(Regex("[/\\\\:*?\"<>|]"), "_").trim()
        return cleaned.ifEmpty { "download" }
    }
}

class StorageException(message: String) : Exception(message)
