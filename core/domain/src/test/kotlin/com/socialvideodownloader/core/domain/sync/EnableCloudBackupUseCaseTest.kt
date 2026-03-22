package com.socialvideodownloader.core.domain.sync

import com.socialvideodownloader.core.domain.repository.DownloadRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EnableCloudBackupUseCaseTest {
    private val authService = mockk<CloudAuthService>(relaxed = true)
    private val preferences = mockk<BackupPreferences>(relaxed = true)
    private val syncManager = mockk<SyncManager>(relaxed = true)
    private val downloadRepository = mockk<DownloadRepository>(relaxed = true)
    private lateinit var useCase: EnableCloudBackupUseCase

    @BeforeEach
    fun setup() {
        useCase = EnableCloudBackupUseCase(authService, preferences, syncManager, downloadRepository)
    }

    @Test
    fun `invoke calls signInWithGoogleCredential with idToken`() =
        runTest {
            useCase("test-id-token")

            coVerify { authService.signInWithGoogleCredential("test-id-token") }
        }

    @Test
    fun `invoke sets backup enabled true and hasEverEnabled true`() =
        runTest {
            useCase("test-id-token")

            coVerify { preferences.setBackupEnabled(true) }
            coVerify { preferences.setHasEverEnabled(true) }
        }

    @Test
    fun `invoke calls processPendingOperations after auth`() =
        runTest {
            useCase("test-id-token")

            coVerifyOrder {
                authService.signInWithGoogleCredential("test-id-token")
                syncManager.processPendingOperations()
            }
        }

    @Test
    fun `first enable backfills existing completed downloads`() =
        runTest {
            coEvery { preferences.hasEverEnabled() } returns false
            val records = listOf(mockk<com.socialvideodownloader.core.domain.model.DownloadRecord>(relaxed = true))
            coEvery { downloadRepository.getCompletedSnapshot() } returns records

            useCase("test-id-token")

            coVerify { syncManager.syncNewRecord(records[0]) }
        }

    @Test
    fun `subsequent enable does not backfill`() =
        runTest {
            coEvery { preferences.hasEverEnabled() } returns true

            useCase("test-id-token")

            coVerify(exactly = 0) { downloadRepository.getCompletedSnapshot() }
        }
}
