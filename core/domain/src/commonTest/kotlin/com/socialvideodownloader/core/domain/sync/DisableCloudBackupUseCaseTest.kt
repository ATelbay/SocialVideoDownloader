package com.socialvideodownloader.core.domain.sync

import com.socialvideodownloader.core.domain.fake.FakeBackupPreferences
import com.socialvideodownloader.core.domain.fake.FakeCloudAuthService
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DisableCloudBackupUseCaseTest {
    private lateinit var preferences: FakeBackupPreferences
    private lateinit var authService: FakeCloudAuthService
    private lateinit var useCase: DisableCloudBackupUseCase

    @BeforeTest
    fun setup() {
        preferences = FakeBackupPreferences()
        authService = FakeCloudAuthService()
        useCase = DisableCloudBackupUseCase(preferences, authService)
    }

    @Test
    fun invokeSetsBackupEnabledToFalse() =
        runTest {
            useCase()

            assertEquals(listOf(false), preferences.setBackupEnabledCalls)
        }

    @Test
    fun invokeCallsSignOutAfterDisablingBackup() =
        runTest {
            useCase()

            assertEquals(listOf(false), preferences.setBackupEnabledCalls)
            assertTrue(authService.signOutCalled)
        }
}
