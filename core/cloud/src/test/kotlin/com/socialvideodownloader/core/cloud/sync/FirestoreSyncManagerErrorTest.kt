package com.socialvideodownloader.core.cloud.sync

import app.cash.turbine.test
import com.socialvideodownloader.core.data.local.DownloadDao
import com.socialvideodownloader.core.data.local.DownloadEntity
import com.socialvideodownloader.core.data.local.SyncQueueDao
import com.socialvideodownloader.core.data.local.SyncQueueEntity
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
import java.io.IOException

class FirestoreSyncManagerErrorTest {
    private val syncQueueDao = mockk<SyncQueueDao>(relaxed = true)
    private val downloadDao = mockk<DownloadDao>(relaxed = true)
    private val cloudBackupRepository = mockk<CloudBackupRepository>(relaxed = true)
    private val encryptionService = mockk<EncryptionService>(relaxed = true)
    private val backupPreferences = mockk<BackupPreferences>(relaxed = true)
    private val cloudAuthService = mockk<CloudAuthService>(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var syncManager: FirestoreSyncManager

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

    private val uploadOp =
        SyncQueueEntity(
            id = 1L,
            downloadId = 1L,
            operation = "UPLOAD",
            createdAt = System.currentTimeMillis(),
            retryCount = 0,
        )

    @BeforeEach
    fun setup() {
        every { backupPreferences.observeIsBackupEnabled() } returns flowOf(true)
        coEvery { cloudBackupRepository.getCloudRecordCount() } returns 0
        coEvery { cloudBackupRepository.getTierLimit() } returns 1000
        coEvery { downloadDao.getById(1L) } returns testDownloadEntity

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
    fun `when offline upload throws IOException, retryCount incremented and syncStatus becomes Paused`() =
        runTest(testDispatcher) {
            coEvery { syncQueueDao.getAll() } returns listOf(uploadOp)
            coEvery { cloudBackupRepository.uploadRecord(any()) } throws IOException("Network unreachable")

            syncManager.observeSyncStatus().test {
                assertEquals(SyncStatus.Idle, awaitItem())

                syncManager.processPendingOperations()

                // Should transition through Syncing
                val syncing = awaitItem()
                assertTrue(syncing is SyncStatus.Syncing)

                // Should become Paused after failure
                val paused = awaitItem()
                assertTrue(paused is SyncStatus.Paused)

                coVerify {
                    syncQueueDao.updateRetry(
                        id = uploadOp.id,
                        retryCount = 1,
                        lastError = any(),
                    )
                }

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `when auth expired, error is propagated without silent re-auth`() =
        runTest(testDispatcher) {
            coEvery { syncQueueDao.getAll() } returns listOf(uploadOp)
            coEvery { cloudBackupRepository.uploadRecord(any()) } throws
                com.google.firebase.auth.FirebaseAuthException("ERROR_USER_TOKEN_EXPIRED", "Token expired")

            syncManager.processPendingOperations()

            // Verify upload was attempted only once (no re-auth retry)
            coVerify(exactly = 1) { cloudBackupRepository.uploadRecord(any()) }
        }

    @Test
    fun `exponential backoff retryCount incremented on each failure`() =
        runTest(testDispatcher) {
            val op = uploadOp.copy(retryCount = 2) // already failed twice
            coEvery { syncQueueDao.getAll() } returns listOf(op)
            coEvery { cloudBackupRepository.uploadRecord(any()) } throws IOException("Still failing")

            syncManager.processPendingOperations()

            // retryCount should be incremented to 3
            coVerify {
                syncQueueDao.updateRetry(
                    id = op.id,
                    retryCount = 3,
                    lastError = any(),
                )
            }
        }

    @Test
    fun `after max 5 retries operation dropped from queue`() =
        runTest(testDispatcher) {
            coEvery { syncQueueDao.getAll() } returns listOf(uploadOp)
            coEvery { cloudBackupRepository.uploadRecord(any()) } throws IOException("Persistent failure")

            syncManager.processPendingOperations()

            // Should call deleteFailedOperations with maxRetries=5
            coVerify { syncQueueDao.deleteFailedOperations(maxRetries = 5) }
        }

    @Test
    fun `when Firestore unavailable SyncStatus Paused emitted`() =
        runTest(testDispatcher) {
            coEvery { syncQueueDao.getAll() } returns listOf(uploadOp)
            coEvery { cloudBackupRepository.uploadRecord(any()) } throws RuntimeException("Firestore unavailable")

            syncManager.observeSyncStatus().test {
                assertEquals(SyncStatus.Idle, awaitItem())

                syncManager.processPendingOperations()

                awaitItem() // Syncing
                val status = awaitItem()
                assertTrue(
                    status is SyncStatus.Paused,
                    "Expected Paused but got $status",
                )

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `processPendingOperations does not throw when upload fails`() =
        runTest(testDispatcher) {
            coEvery { syncQueueDao.getAll() } returns listOf(uploadOp)
            coEvery { cloudBackupRepository.uploadRecord(any()) } throws RuntimeException("Cloud error")

            // Should not throw — cloud failures are swallowed
            syncManager.processPendingOperations()
        }
}
