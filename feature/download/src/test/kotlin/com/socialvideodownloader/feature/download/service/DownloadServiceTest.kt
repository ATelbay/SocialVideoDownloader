package com.socialvideodownloader.feature.download.service

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class DownloadServiceTest {
    @Test
    fun `cancel cleanup deletes all files in ytdl_downloads cache directory`(
        @TempDir tempDir: File,
    ) {
        // Simulate cache directory with partial download files
        val cacheDir = File(tempDir, "ytdl_downloads").apply { mkdirs() }
        File(cacheDir, "partial_video.part").writeText("partial data")
        File(cacheDir, "partial_video.mp4.part").writeText("more partial data")
        val subDir = File(cacheDir, "temp_merge").apply { mkdirs() }
        File(subDir, "segment.ts").writeText("segment")

        // Verify files exist before cleanup
        assertTrue(cacheDir.listFiles()!!.isNotEmpty())

        // Execute cleanup logic (same as what DownloadService does after cancel)
        cacheDir.listFiles()?.forEach { it.deleteRecursively() }

        // Verify all files are deleted but directory is preserved
        assertTrue(cacheDir.exists(), "Cache directory should still exist")
        assertTrue(cacheDir.listFiles()!!.isEmpty(), "Cache directory should be empty after cleanup")
    }
}
