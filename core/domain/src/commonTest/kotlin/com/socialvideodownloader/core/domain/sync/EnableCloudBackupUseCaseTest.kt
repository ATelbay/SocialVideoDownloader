package com.socialvideodownloader.core.domain.sync

import com.socialvideodownloader.core.domain.fake.FakeBackupPreferences
import com.socialvideodownloader.core.domain.fake.FakeCloudAuthService
import com.socialvideodownloader.core.domain.fake.FakeDownloadRepository
import com.socialvideodownloader.core.domain.fake.FakeSyncManager
import com.socialvideodownloader.core.domain.model.DownloadRecord
import com.socialvideodownloader.core.domain.model.DownloadStatus
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EnableCloudBackupUseCaseTest {
    private lateinit var authService: FakeCloudAuthService
    private lateinit var preferences: FakeBackupPreferences
    private lateinit var syncManager: FakeSyncManager
    private lateinit var downloadRepository: FakeDownloadRepository
    private lateinit var useCase: EnableCloudBackupUseCase

    @BeforeTest
    fun setup() {
        authService = FakeCloudAuthService()
        preferences = FakeBackupPreferences()
        syncManager = FakeSyncManager()
        downloadRepository = FakeDownloadRepository()
        useCase = EnableCloudBackupUseCase(authService, preferences, syncManager, downloadRepository)
    }

    @Test
    fun invokeCallsSignInWithGoogleCredentialWithIdToken() =
        runTest {
            useCase("test-id-token")

            assertEquals(listOf("test-id-token"), authService.signedInIdTokens)
        }

    @Test
    fun invokeSetsBackupEnabledTrueAndHasEverEnabledTrue() =
        runTest {
            useCase("test-id-token")

            assertEquals(listOf(true), preferences.setBackupEnabledCalls)
            assertEquals(listOf(true), preferences.setHasEverEnabledCalls)
        }

    @Test
    fun invokeCallsProcessPendingOperationsAfterAuth() =
        runTest {
            useCase("test-id-token")

            assertEquals(listOf("test-id-token"), authService.signedInIdTokens)
            assertTrue(syncManager.processPendingCalled)
        }

    @Test
    fun firstEnableBackfillsExistingCompletedDownloads() =
        runTest {
            preferences.setHasEverEnabledState(false)
            val record =
                DownloadRecord(
                    id = 1L,
                    sourceUrl = "https://example.com/video",
                    videoTitle = "Test",
                    status = DownloadStatus.COMPLETED,
                    createdAt = 1000L,
                )
            downloadRepository.setRecords(listOf(record))

            useCase("test-id-token")

            assertEquals(1, syncManager.syncedRecords.size)
            assertEquals(record.sourceUrl, syncManager.syncedRecords[0].sourceUrl)
        }

    @Test
    fun subsequentEnableDoesNotBackfill() =
        runTest {
            preferences.setHasEverEnabledState(true)

            useCase("test-id-token")

            assertTrue(syncManager.syncedRecords.isEmpty())
        }
}
