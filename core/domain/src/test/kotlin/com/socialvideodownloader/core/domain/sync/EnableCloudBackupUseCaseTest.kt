package com.socialvideodownloader.core.domain.sync

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EnableCloudBackupUseCaseTest {
    private val authService = mockk<CloudAuthService>(relaxed = true)
    private val preferences = mockk<BackupPreferences>(relaxed = true)
    private val syncManager = mockk<SyncManager>(relaxed = true)
    private lateinit var useCase: EnableCloudBackupUseCase

    @BeforeEach
    fun setup() {
        useCase = EnableCloudBackupUseCase(authService, preferences, syncManager)
    }

    @Test
    fun `invoke calls signInAnonymously`() =
        runTest {
            coEvery { preferences.hasEverEnabled() } returns false
            coEvery { preferences.observeIsBackupEnabled() } returns flowOf(false)

            useCase()

            coVerify { authService.signInAnonymously() }
        }

    @Test
    fun `invoke sets backup enabled true and hasEverEnabled true`() =
        runTest {
            coEvery { preferences.hasEverEnabled() } returns false
            coEvery { preferences.observeIsBackupEnabled() } returns flowOf(false)

            useCase()

            coVerify { preferences.setBackupEnabled(true) }
            coVerify { preferences.setHasEverEnabled(true) }
        }

    @Test
    fun `invoke calls processPendingOperations after auth`() =
        runTest {
            coEvery { preferences.hasEverEnabled() } returns false
            coEvery { preferences.observeIsBackupEnabled() } returns flowOf(false)

            useCase()

            coVerifyOrder {
                authService.signInAnonymously()
                syncManager.processPendingOperations()
            }
        }

    @Test
    fun `invoke is no-op when already enabled`() =
        runTest {
            coEvery { preferences.hasEverEnabled() } returns true
            coEvery { preferences.observeIsBackupEnabled() } returns flowOf(true)

            useCase()

            coVerify(exactly = 0) { authService.signInAnonymously() }
            coVerify(exactly = 0) { syncManager.processPendingOperations() }
        }
}
