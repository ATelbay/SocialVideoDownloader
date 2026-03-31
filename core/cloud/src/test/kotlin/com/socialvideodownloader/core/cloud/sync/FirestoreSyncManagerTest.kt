package com.socialvideodownloader.core.cloud.sync

import app.cash.turbine.test
import com.socialvideodownloader.core.data.local.DownloadDao
import com.socialvideodownloader.core.data.local.DownloadEntity
import com.socialvideodownloader.core.data.local.SyncQueueDao
import com.socialvideodownloader.core.data.local.SyncQueueEntity
import com.socialvideodownloader.core.domain.model.DownloadRecord
import com.socialvideodownloader.core.domain.model.DownloadStatus
import com.socialvideodownloader.core.domain.model.SyncStatus
import com.socialvideodownloader.core.domain.repository.CloudBackupRepository
import com.socialvideodownloader.core.domain.sync.BackupPreferences
import com.socialvideodownloader.core.domain.sync.CloudAuthService
import com.socialvideodownloader.core.domain.sync.EncryptionService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FirestoreSyncManagerTest {
    private val syncQueueDao = mockk<SyncQueueDao>(relaxed = true)
    private val downloadDao = mockk<DownloadDao>(relaxed = true)
    private val cloudBackupRepository = mockk<CloudBackupRepository>(relaxed = true)
    private val encryptionService = mockk<EncryptionService>(relaxed = true)
    private val backupPreferences = mockk<BackupPreferences>(relaxed = true)
    private val cloudAuthService = mockk<CloudAuthService>(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var syncManager: FirestoreSyncManager

    private val testRecord =
        DownloadRecord(
            id = 1L,
            sourceUrl = "https://example.com/video",
            videoTitle = "Test Video",
            status = DownloadStatus.COMPLETED,
            createdAt = 1000L,
        )

    private val testDownloadEntity =
        DownloadEntity(
            id = 1L,
            sourceUrl = "https://example.com/video",
            videoTitle = "Test Video",
            thumbnailUrl = null,
            formatLabel = "1080p",
            filePath = null,
            mediaStoreUri = null,
            status = "COMPLETED",
            createdAt = 1000L,
            completedAt = null,
            fileSizeBytes = null,
            syncStatus = "NOT_SYNCED",
        )

    @BeforeEach
    fun setup() {
        every { backupPreferences.observeIsBackupEnabled() } returns flowOf(true)
        coEvery { cloudBackupRepository.getCloudRecordCount() } returns 0
        coEvery { cloudBackupRepository.getTierLimit() } returns 1000
        coEvery { downloadDao.getById(testRecord.id) } returns testDownloadEntity

        syncManager =
            FirestoreSyncManager(
                syncQueueDao = syncQueueDao,
                downloadDao = downloadDao,
                cloudBackupRepository = cloudBackupRepository,
                encryptionService = encryptionService,
                backupPreferences = backupPreferences,
                cloudAuthService = cloudAuthService,
                ioDispatcher = testDispatcher,
            )
    }

    @Test
    fun `processPendingOperations processes UPLOAD operations from queue`() =
        runTest(testDispatcher) {
            val uploadOp =
                SyncQueueEntity(
                    id = 1L,
                    downloadId = testRecord.id,
                    operation = "UPLOAD",
                    createdAt = System.currentTimeMillis(),
                )
            coEvery { syncQueueDao.getAll() } returns listOf(uploadOp)
            coEvery { cloudBackupRepository.uploadRecord(any()) } returns true

            syncManager.processPendingOperations()

            coVerify { cloudBackupRepository.uploadRecord(any()) }
        }

    @Test
    fun `processPendingOperations deletes queue entry on successful UPLOAD`() =
        runTest(testDispatcher) {
            val uploadOp =
                SyncQueueEntity(
                    id = 1L,
                    downloadId = testRecord.id,
                    operation = "UPLOAD",
                    createdAt = System.currentTimeMillis(),
                )
            coEvery { syncQueueDao.getAll() } returns listOf(uploadOp)
            coEvery { cloudBackupRepository.uploadRecord(any()) } returns true

            syncManager.processPendingOperations()

            coVerify { syncQueueDao.deleteById(1L) }
        }

    @Test
    fun `processPendingOperations processes DELETE operations from queue`() =
        runTest(testDispatcher) {
            val deleteOp =
                SyncQueueEntity(
                    id = 2L,
                    downloadId = testRecord.id,
                    operation = "DELETE",
                    createdAt = System.currentTimeMillis(),
                )
            coEvery { syncQueueDao.getAll() } returns listOf(deleteOp)
            coEvery { cloudBackupRepository.deleteRecord(any()) } returns true

            syncManager.processPendingOperations()

            coVerify { cloudBackupRepository.deleteRecord(any()) }
            coVerify { syncQueueDao.deleteById(2L) }
        }

    @Test
    fun `syncNewRecord encrypts and uploads record`() =
        runTest(testDispatcher) {
            coEvery { cloudBackupRepository.uploadRecord(testRecord) } returns true

            syncManager.syncNewRecord(testRecord)

            coVerify { cloudBackupRepository.uploadRecord(testRecord) }
        }

    @Test
    fun `syncNewRecord triggers LRU eviction when at tier limit`() =
        runTest(testDispatcher) {
            coEvery { cloudBackupRepository.getCloudRecordCount() } returns 1000
            coEvery { cloudBackupRepository.getTierLimit() } returns 1000
            coEvery { cloudBackupRepository.uploadRecord(testRecord) } returns true

            syncManager.syncNewRecord(testRecord)

            coVerify { cloudBackupRepository.evictOldestRecords(1) }
        }

    @Test
    fun `queueDeletion inserts DELETE operation in sync queue`() =
        runTest(testDispatcher) {
            syncManager.queueDeletion(testRecord)

            coVerify {
                syncQueueDao.insert(
                    match { it.downloadId == testRecord.id && it.operation == "DELETE" },
                )
            }
        }

    @Test
    fun `observeSyncStatus emits Idle initially`() =
        runTest(testDispatcher) {
            syncManager.observeSyncStatus().test {
                val status = awaitItem()
                assertTrue(status is SyncStatus.Idle)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `observeSyncStatus transitions Idle to Syncing to Synced during processPendingOperations`() =
        runTest(testDispatcher) {
            val uploadOp =
                SyncQueueEntity(
                    id = 1L,
                    downloadId = testRecord.id,
                    operation = "UPLOAD",
                    createdAt = System.currentTimeMillis(),
                )
            coEvery { syncQueueDao.getAll() } returns listOf(uploadOp)
            coEvery { cloudBackupRepository.uploadRecord(any()) } returns true

            syncManager.observeSyncStatus().test {
                assertEquals(SyncStatus.Idle, awaitItem())

                syncManager.processPendingOperations()

                val syncing = awaitItem()
                assertTrue(syncing is SyncStatus.Syncing)

                val synced = awaitItem()
                assertTrue(synced is SyncStatus.Synced)

                cancelAndIgnoreRemainingEvents()
            }
        }
}
