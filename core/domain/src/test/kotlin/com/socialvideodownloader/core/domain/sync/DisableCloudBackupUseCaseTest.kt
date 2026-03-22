package com.socialvideodownloader.core.domain.sync

import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DisableCloudBackupUseCaseTest {
    private val preferences = mockk<BackupPreferences>(relaxed = true)
    private val authService = mockk<CloudAuthService>(relaxed = true)
    private lateinit var useCase: DisableCloudBackupUseCase

    @BeforeEach
    fun setup() {
        useCase = DisableCloudBackupUseCase(preferences, authService)
    }

    @Test
    fun `invoke sets backup enabled to false`() =
        runTest {
            useCase()

            coVerify { preferences.setBackupEnabled(false) }
        }

    @Test
    fun `invoke calls signOut after disabling backup`() =
        runTest {
            useCase()

            coVerifyOrder {
                preferences.setBackupEnabled(false)
                authService.signOut()
            }
        }
}
