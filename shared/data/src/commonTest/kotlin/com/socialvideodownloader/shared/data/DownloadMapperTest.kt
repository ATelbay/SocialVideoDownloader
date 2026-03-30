package com.socialvideodownloader.shared.data

import com.socialvideodownloader.core.domain.model.DownloadRecord
import com.socialvideodownloader.core.domain.model.DownloadStatus
import com.socialvideodownloader.shared.data.local.DownloadEntity
import com.socialvideodownloader.shared.data.local.toDomain
import com.socialvideodownloader.shared.data.local.toEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DownloadMapperTest {
    @Test
    fun entityToDomainMapsAllFields() {
        val entity =
            DownloadEntity(
                id = 42,
                sourceUrl = "https://youtube.com/watch?v=abc",
                videoTitle = "Test Video",
                thumbnailUrl = "https://img.youtube.com/vi/abc/0.jpg",
                formatLabel = "1080p mp4",
                filePath = "/storage/downloads/test.mp4",
                mediaStoreUri = "content://media/external/downloads/123",
                status = "COMPLETED",
                createdAt = 1000L,
                completedAt = 2000L,
                fileSizeBytes = 5_000_000L,
                syncStatus = "SYNCED",
            )

        val record = entity.toDomain()

        assertEquals(42, record.id)
        assertEquals("https://youtube.com/watch?v=abc", record.sourceUrl)
        assertEquals("Test Video", record.videoTitle)
        assertEquals("https://img.youtube.com/vi/abc/0.jpg", record.thumbnailUrl)
        assertEquals("1080p mp4", record.formatLabel)
        assertEquals("/storage/downloads/test.mp4", record.filePath)
        assertEquals("content://media/external/downloads/123", record.mediaStoreUri)
        assertEquals(DownloadStatus.COMPLETED, record.status)
        assertEquals(1000L, record.createdAt)
        assertEquals(2000L, record.completedAt)
        assertEquals(5_000_000L, record.fileSizeBytes)
        assertEquals("SYNCED", record.syncStatus)
    }

    @Test
    fun entityToDomainFallsBackToFailedForUnknownStatus() {
        val entity =
            DownloadEntity(
                id = 1,
                sourceUrl = "https://example.com",
                videoTitle = "Test",
                thumbnailUrl = null,
                filePath = null,
                status = "INVALID_STATUS",
                createdAt = 1000L,
                completedAt = null,
                fileSizeBytes = null,
            )

        val record = entity.toDomain()
        assertEquals(DownloadStatus.FAILED, record.status)
    }

    @Test
    fun domainToEntityMapsAllFields() {
        val record =
            DownloadRecord(
                id = 7,
                sourceUrl = "https://instagram.com/reel/xyz",
                videoTitle = "Reel",
                thumbnailUrl = null,
                formatLabel = "720p mp4",
                filePath = null,
                mediaStoreUri = null,
                status = DownloadStatus.DOWNLOADING,
                createdAt = 3000L,
                completedAt = null,
                fileSizeBytes = null,
                syncStatus = "NOT_SYNCED",
            )

        val entity = record.toEntity()

        assertEquals(7, entity.id)
        assertEquals("https://instagram.com/reel/xyz", entity.sourceUrl)
        assertEquals("Reel", entity.videoTitle)
        assertNull(entity.thumbnailUrl)
        assertEquals("720p mp4", entity.formatLabel)
        assertNull(entity.filePath)
        assertNull(entity.mediaStoreUri)
        assertEquals("DOWNLOADING", entity.status)
        assertEquals(3000L, entity.createdAt)
        assertNull(entity.completedAt)
        assertNull(entity.fileSizeBytes)
        assertEquals("NOT_SYNCED", entity.syncStatus)
    }

    @Test
    fun roundTripPreservesData() {
        val original =
            DownloadRecord(
                id = 99,
                sourceUrl = "https://tiktok.com/@user/video/123",
                videoTitle = "TikTok Video",
                thumbnailUrl = "https://p16.tiktokcdn.com/img.jpg",
                formatLabel = "best",
                filePath = "/downloads/tiktok.mp4",
                mediaStoreUri = "content://media/123",
                status = DownloadStatus.COMPLETED,
                createdAt = 100L,
                completedAt = 200L,
                fileSizeBytes = 1_000_000L,
                syncStatus = "PENDING_UPLOAD",
            )

        val roundTripped = original.toEntity().toDomain()

        assertEquals(original, roundTripped)
    }
}
