package com.socialvideodownloader.core.domain.sync

import com.socialvideodownloader.core.domain.fake.FakeCloudBackupRepository
import com.socialvideodownloader.core.domain.fake.FakeDownloadRepository
import com.socialvideodownloader.core.domain.model.DownloadRecord
import com.socialvideodownloader.core.domain.model.DownloadStatus
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class RestoreFromCloudUseCaseTest {
    private lateinit var cloudBackupRepository: FakeCloudBackupRepository
    private lateinit var downloadRepository: FakeDownloadRepository
    private lateinit var useCase: RestoreFromCloudUseCase

    @BeforeTest
    fun setup() {
        cloudBackupRepository = FakeCloudBackupRepository()
        downloadRepository = FakeDownloadRepository()
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
    fun fetchesAllCloudRecordsAndInsertsNonDuplicatesIntoLocalDb() =
        runTest {
            val cloudRecord = record("https://example.com/video1", 1000L)
            cloudBackupRepository.records.add(cloudRecord)
            downloadRepository.setRecords(emptyList())

            val result = useCase()

            assertEquals(1, result.restored)
            assertEquals(0, result.skipped)
            assertEquals(0, result.failed)
            assertEquals(1, downloadRepository.insertedRecords.size)
        }

    @Test
    fun dedupRecordsWithSameSourceUrlAndCreatedAtAreSkipped() =
        runTest {
            val existing = record("https://example.com/video1", 1000L)
            val cloudRecord = record("https://example.com/video1", 1000L)
            downloadRepository.setRecords(listOf(existing))
            cloudBackupRepository.records.add(cloudRecord)

            val result = useCase()

            assertEquals(0, result.restored)
            assertEquals(1, result.skipped)
            assertEquals(0, result.failed)
            assertEquals(0, downloadRepository.insertedRecords.size)
        }

    @Test
    fun dedupSameSourceUrlButDifferentCreatedAtIsNotADuplicate() =
        runTest {
            val existing = record("https://example.com/video1", 1000L)
            val cloudRecord = record("https://example.com/video1", 2000L)
            downloadRepository.setRecords(listOf(existing))
            cloudBackupRepository.records.add(cloudRecord)

            val result = useCase()

            assertEquals(1, result.restored)
            assertEquals(0, result.skipped)
        }

    @Test
    fun returnsRestoreResultWithCorrectCounts() =
        runTest {
            val existing = record("https://example.com/dup", 1000L)
            val cloudDuplicate = record("https://example.com/dup", 1000L)
            val cloudNew = record("https://example.com/new", 2000L)
            downloadRepository.setRecords(listOf(existing))
            cloudBackupRepository.records.addAll(listOf(cloudDuplicate, cloudNew))

            val result = useCase()

            assertEquals(1, result.restored)
            assertEquals(1, result.skipped)
            assertEquals(0, result.failed)
            assertNull(result.error)
        }

    @Test
    fun whenDecryptionFailsReturnsErrorResult() =
        runTest {
            val errorMessage = "Encryption key no longer available"
            cloudBackupRepository.fetchException = IllegalStateException(errorMessage)

            val result = useCase()

            assertEquals(0, result.restored)
            assertEquals(0, result.skipped)
            assertEquals(0, result.failed)
            assertNotNull(result.error)
            assertEquals(errorMessage, result.error)
        }

    @Test
    fun insertFailureCountsAsFailed() =
        runTest {
            val cloudRecord = record("https://example.com/video1", 1000L)
            cloudBackupRepository.records.add(cloudRecord)
            downloadRepository.setRecords(emptyList())
            downloadRepository.insertException = RuntimeException("DB error")

            val result = useCase()

            assertEquals(0, result.restored)
            assertEquals(0, result.skipped)
            assertEquals(1, result.failed)
        }
}
