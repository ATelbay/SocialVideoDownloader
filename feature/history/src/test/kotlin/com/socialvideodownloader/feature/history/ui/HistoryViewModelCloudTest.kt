package com.socialvideodownloader.feature.history.ui

import app.cash.turbine.test
import com.socialvideodownloader.core.domain.model.SyncStatus
import com.socialvideodownloader.core.domain.repository.BillingRepository
import com.socialvideodownloader.core.domain.sync.BackupPreferences
import com.socialvideodownloader.core.domain.sync.CloudAuthService
import com.socialvideodownloader.core.domain.sync.DisableCloudBackupUseCase
import com.socialvideodownloader.core.domain.sync.EnableCloudBackupUseCase
import com.socialvideodownloader.core.domain.sync.ObserveCloudCapacityUseCase
import com.socialvideodownloader.core.domain.sync.RestoreFromCloudUseCase
import com.socialvideodownloader.core.domain.sync.SyncManager
import com.socialvideodownloader.feature.history.testdouble.FakeDownloadRepository
import com.socialvideodownloader.feature.history.testdouble.FakeHistoryFileManager
import com.socialvideodownloader.feature.history.testutil.MainDispatcherRule
import com.socialvideodownloader.shared.data.platform.PlatformClipboard
import com.socialvideodownloader.shared.feature.history.HistoryEffect.LaunchGoogleSignIn
import com.socialvideodownloader.shared.feature.history.HistoryIntent.DismissSignInError
import com.socialvideodownloader.shared.feature.history.HistoryIntent.SignInWithGoogle
import com.socialvideodownloader.shared.feature.history.HistoryIntent.SignOutCloud
import com.socialvideodownloader.shared.feature.history.HistoryIntent.ToggleCloudBackup
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class HistoryViewModelCloudTest {

    @RegisterExtension
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeDownloadRepository()
    private val fileManager = FakeHistoryFileManager()
    private val observeCloudCapacity = mockk<ObserveCloudCapacityUseCase>()
    private val billingRepository = mockk<BillingRepository>(relaxed = true)
    private val enableCloudBackupUseCase = mockk<EnableCloudBackupUseCase>(relaxed = true)
    private val disableCloudBackupUseCase = mockk<DisableCloudBackupUseCase>(relaxed = true)
    private val syncManager = mockk<SyncManager>(relaxed = true)
    private val backupPreferences = mockk<BackupPreferences>(relaxed = true)
    private val restoreFromCloudUseCase = mockk<RestoreFromCloudUseCase>(relaxed = true)
    private val cloudAuthService = mockk<CloudAuthService>(relaxed = true)
    private val clipboard = mockk<PlatformClipboard>(relaxed = true)

    private val isBackupEnabledFlow = MutableStateFlow(false)
    private val syncStatusFlow = MutableStateFlow<SyncStatus>(SyncStatus.Idle)

    @BeforeEach
    fun setup() {
        every { observeCloudCapacity() } returns flowOf()
        every { backupPreferences.observeIsBackupEnabled() } returns isBackupEnabledFlow
        every { syncManager.observeSyncStatus() } returns syncStatusFlow
        every { cloudAuthService.isAuthenticated() } returns false
        every { cloudAuthService.getDisplayName() } returns null
        every { cloudAuthService.getPhotoUrl() } returns null
    }

    private fun createViewModel() = HistoryViewModel(
        downloadRepository = repository,
        fileManager = fileManager,
        observeCloudCapacity = observeCloudCapacity,
        billingRepository = billingRepository,
        enableCloudBackupUseCase = enableCloudBackupUseCase,
        disableCloudBackupUseCase = disableCloudBackupUseCase,
        syncManager = syncManager,
        backupPreferences = backupPreferences,
        restoreFromCloudUseCase = restoreFromCloudUseCase,
        cloudAuthService = cloudAuthService,
        clipboard = clipboard,
    )

    @Test
    fun `initial state has isCloudBackupEnabled false`() = runTest {
        val vm = createViewModel()
        val state = vm.cloudBackupState.value
        assertFalse(state.isCloudBackupEnabled)
    }

    @Test
    fun `initial state has syncStatus Idle`() = runTest {
        val vm = createViewModel()
        val state = vm.cloudBackupState.value
        assertEquals(SyncStatus.Idle, state.syncStatus)
    }

    @Test
    fun `initial state has isSignedIn false when not authenticated`() = runTest {
        val vm = createViewModel()
        val state = vm.cloudBackupState.value
        assertFalse(state.isSignedIn)
    }

    @Test
    fun `ToggleCloudBackup when not signed in emits LaunchGoogleSignIn effect`() = runTest {
        every { cloudAuthService.isAuthenticated() } returns false
        val vm = createViewModel()

        vm.effect.test {
            vm.onIntent(ToggleCloudBackup)
            assertEquals(LaunchGoogleSignIn, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `ToggleCloudBackup when signed in and enabled calls DisableCloudBackupUseCase`() = runTest {
        every { cloudAuthService.isAuthenticated() } returns true
        isBackupEnabledFlow.value = true
        val vm = createViewModel()

        vm.onIntent(ToggleCloudBackup)

        coVerify { disableCloudBackupUseCase() }
    }

    @Test
    fun `SignInWithGoogle calls enableCloudBackupUseCase with idToken`() = runTest {
        every { cloudAuthService.isAuthenticated() } returns false
        coEvery { enableCloudBackupUseCase(any()) } coAnswers {
            every { cloudAuthService.isAuthenticated() } returns true
        }
        val vm = createViewModel()

        vm.onIntent(SignInWithGoogle("test-token"))

        coVerify { enableCloudBackupUseCase("test-token") }
    }

    @Test
    fun `SignInWithGoogle on failure sets signInError`() = runTest {
        every { cloudAuthService.isAuthenticated() } returns false
        coEvery { enableCloudBackupUseCase(any()) } throws RuntimeException("auth failed")
        val vm = createViewModel()

        vm.onIntent(SignInWithGoogle("bad-token"))

        vm.cloudBackupState.test {
            val state = awaitItem()
            assertFalse(state.isSigningIn)
            assertEquals("auth failed", state.signInError)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `SignOutCloud calls disableCloudBackupUseCase and updates auth state`() = runTest {
        every { cloudAuthService.isAuthenticated() } returns true
        val vm = createViewModel()

        coEvery { disableCloudBackupUseCase() } coAnswers {
            every { cloudAuthService.isAuthenticated() } returns false
        }

        vm.onIntent(SignOutCloud)

        coVerify { disableCloudBackupUseCase() }
    }

    @Test
    fun `DismissSignInError clears signInError`() = runTest {
        every { cloudAuthService.isAuthenticated() } returns false
        coEvery { enableCloudBackupUseCase(any()) } throws RuntimeException("auth failed")
        val vm = createViewModel()

        vm.onIntent(SignInWithGoogle("bad-token"))
        vm.onIntent(DismissSignInError)

        vm.cloudBackupState.test {
            val state = awaitItem()
            assertNull(state.signInError)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `cloudBackupState updates isCloudBackupEnabled from BackupPreferences`() = runTest {
        val vm = createViewModel()

        vm.cloudBackupState.test {
            assertFalse(awaitItem().isCloudBackupEnabled)

            isBackupEnabledFlow.value = true
            assertTrue(awaitItem().isCloudBackupEnabled)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `cloudBackupState updates syncStatus from SyncManager observation`() = runTest {
        val vm = createViewModel()

        vm.cloudBackupState.test {
            assertEquals(SyncStatus.Idle, awaitItem().syncStatus)

            syncStatusFlow.value = SyncStatus.Syncing
            assertEquals(SyncStatus.Syncing, awaitItem().syncStatus)

            val syncedStatus = SyncStatus.Synced(lastSyncTimestamp = 12345L)
            syncStatusFlow.value = syncedStatus
            assertEquals(syncedStatus, awaitItem().syncStatus)

            cancelAndIgnoreRemainingEvents()
        }
    }
}
