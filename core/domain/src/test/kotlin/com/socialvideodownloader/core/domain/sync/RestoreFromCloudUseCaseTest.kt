package com.socialvideodownloader.core.domain.sync

import com.socialvideodownloader.core.domain.model.DownloadRecord
import com.socialvideodownloader.core.domain.model.DownloadStatus
import com.socialvideodownloader.core.domain.repository.CloudBackupRepository
import com.socialvideodownloader.core.domain.repository.DownloadRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RestoreFromCloudUseCaseTest {
    private val cloudBackupRepository = mockk<CloudBackupRepository>()
    private val downloadRepository = mockk<DownloadRepository>(relaxed = true)
    private lateinit var useCase: RestoreFromCloudUseCase

    @BeforeEach
    fun setup() {
        useCase = RestoreFromCloudUseCase(cloudBackupRepository, downloadRepository)
    }

    private fun record(
        sourceUrl: String,
        createdAt: Long,
    ) = DownloadRecord(
        id = 0,
        sourceUrl = sourceUrl,
        videoTitle = "Title",
        status = DownloadStatus.COMPLETED,
        createdAt = createdAt,
    )

    @Test
    fun `fetches all cloud records and inserts non-duplicates into local DB`() =
        runTest {
            val cloudRecord = record("https://example.com/video1", 1000L)
            coEvery { cloudBackupRepository.fetchAllRecords() } returns listOf(cloudRecord)
            every { downloadRepository.getAll() } returns flowOf(emptyList())

            val result = useCase()

            coVerify { downloadRepository.insert(cloudRecord) }
            assertEquals(1, result.restored)
            assertEquals(0, result.skipped)
            assertEquals(0, result.failed)
        }

    @Test
    fun `dedup - records with same sourceUrl AND createdAt are skipped`() =
        runTest {
            val existing = record("https://example.com/video1", 1000L)
            val cloudRecord = record("https://example.com/video1", 1000L)
            coEvery { cloudBackupRepository.fetchAllRecords() } returns listOf(cloudRecord)
            every { downloadRepository.getAll() } returns flowOf(listOf(existing))

            val result = useCase()

            coVerify(exactly = 0) { downloadRepository.insert(any()) }
            assertEquals(0, result.restored)
            assertEquals(1, result.skipped)
            assertEquals(0, result.failed)
        }

    @Test
    fun `dedup - same sourceUrl but different createdAt is not a duplicate`() =
        runTest {
            val existing = record("https://example.com/video1", 1000L)
            val cloudRecord = record("https://example.com/video1", 2000L)
            coEvery { cloudBackupRepository.fetchAllRecords() } returns listOf(cloudRecord)
            every { downloadRepository.getAll() } returns flowOf(listOf(existing))

            val result = useCase()

            coVerify { downloadRepository.insert(cloudRecord) }
            assertEquals(1, result.restored)
            assertEquals(0, result.skipped)
        }

    @Test
    fun `returns RestoreResult with correct counts`() =
        runTest {
            val existing = record("https://example.com/dup", 1000L)
            val cloudDuplicate = record("https://example.com/dup", 1000L)
            val cloudNew = record("https://example.com/new", 2000L)
            coEvery { cloudBackupRepository.fetchAllRecords() } returns listOf(cloudDuplicate, cloudNew)
            every { downloadRepository.getAll() } returns flowOf(listOf(existing))

            val result = useCase()

            assertEquals(1, result.restored)
            assertEquals(1, result.skipped)
            assertEquals(0, result.failed)
            assertNull(result.error)
        }

    @Test
    fun `when decryption fails (fetchAllRecords throws), returns error result`() =
        runTest {
            val errorMessage = "Encryption key no longer available"
            coEvery { cloudBackupRepository.fetchAllRecords() } throws IllegalStateException(errorMessage)

            val result = useCase()

            assertEquals(0, result.restored)
            assertEquals(0, result.skipped)
            assertEquals(0, result.failed)
            assertNotNull(result.error)
            assertEquals(errorMessage, result.error)
        }

    @Test
    fun `insert failure counts as failed`() =
        runTest {
            val cloudRecord = record("https://example.com/video1", 1000L)
            coEvery { cloudBackupRepository.fetchAllRecords() } returns listOf(cloudRecord)
            every { downloadRepository.getAll() } returns flowOf(emptyList())
            coEvery { downloadRepository.insert(any()) } throws RuntimeException("DB error")

            val result = useCase()

            assertEquals(0, result.restored)
            assertEquals(0, result.skipped)
            assertEquals(1, result.failed)
        }
}
