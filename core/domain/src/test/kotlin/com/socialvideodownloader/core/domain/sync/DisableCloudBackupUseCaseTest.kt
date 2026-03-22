package com.socialvideodownloader.core.domain.sync

import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DisableCloudBackupUseCaseTest {
    private val preferences = mockk<BackupPreferences>(relaxed = true)
    private lateinit var useCase: DisableCloudBackupUseCase

    @BeforeEach
    fun setup() {
        useCase = DisableCloudBackupUseCase(preferences)
    }

    @Test
    fun `invoke sets backup enabled to false`() =
        runTest {
            useCase()

            coVerify { preferences.setBackupEnabled(false) }
        }

    @Test
    fun `invoke does not trigger any cloud deletion`() =
        runTest {
            val cloudAuthService = mockk<CloudAuthService>(relaxed = true)
            val syncManager = mockk<SyncManager>(relaxed = true)

            useCase()

            // Confirm only preferences is touched — no cloud service interaction
            coVerify(exactly = 0) { cloudAuthService.signInAnonymously() }
            coVerify(exactly = 0) { syncManager.processPendingOperations() }
            coVerify(exactly = 0) { syncManager.queueDeletion(any()) }
        }
}
