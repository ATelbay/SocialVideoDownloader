package com.socialvideodownloader.feature.history.ui

import app.cash.turbine.test
import com.socialvideodownloader.core.domain.model.SyncStatus
import com.socialvideodownloader.core.domain.repository.BillingRepository
import com.socialvideodownloader.core.domain.sync.BackupPreferences
import com.socialvideodownloader.core.domain.sync.DisableCloudBackupUseCase
import com.socialvideodownloader.core.domain.sync.EnableCloudBackupUseCase
import com.socialvideodownloader.core.domain.sync.ObserveCloudCapacityUseCase
import com.socialvideodownloader.core.domain.sync.SyncManager
import com.socialvideodownloader.feature.history.domain.DeleteHistoryItemUseCase
import com.socialvideodownloader.feature.history.domain.ObserveHistoryItemsUseCase
import com.socialvideodownloader.feature.history.testutil.MainDispatcherRule
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class HistoryViewModelCloudTest {

    @RegisterExtension
    val mainDispatcherRule = MainDispatcherRule()

    private val observeHistoryItems = mockk<ObserveHistoryItemsUseCase>()
    private val deleteHistoryItem = mockk<DeleteHistoryItemUseCase>(relaxed = true)
    private val observeCloudCapacity = mockk<ObserveCloudCapacityUseCase>()
    private val billingRepository = mockk<BillingRepository>(relaxed = true)
    private val enableCloudBackupUseCase = mockk<EnableCloudBackupUseCase>(relaxed = true)
    private val disableCloudBackupUseCase = mockk<DisableCloudBackupUseCase>(relaxed = true)
    private val syncManager = mockk<SyncManager>(relaxed = true)
    private val backupPreferences = mockk<BackupPreferences>(relaxed = true)

    private val isBackupEnabledFlow = MutableStateFlow(false)
    private val syncStatusFlow = MutableStateFlow<SyncStatus>(SyncStatus.Idle)

    @BeforeEach
    fun setup() {
        every { observeHistoryItems() } returns flowOf(emptyList())
        every { observeCloudCapacity() } returns flowOf()
        every { backupPreferences.observeIsBackupEnabled() } returns isBackupEnabledFlow
        every { syncManager.observeSyncStatus() } returns syncStatusFlow
    }

    private fun createViewModel() = HistoryViewModel(
        observeHistoryItems = observeHistoryItems,
        deleteHistoryItem = deleteHistoryItem,
        observeCloudCapacity = observeCloudCapacity,
        billingRepository = billingRepository,
        enableCloudBackupUseCase = enableCloudBackupUseCase,
        disableCloudBackupUseCase = disableCloudBackupUseCase,
        syncManager = syncManager,
        backupPreferences = backupPreferences,
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
    fun `ToggleCloudBackup when off calls EnableCloudBackupUseCase`() = runTest {
        isBackupEnabledFlow.value = false
        val vm = createViewModel()

        vm.onIntent(HistoryIntent.ToggleCloudBackup)

        coVerify { enableCloudBackupUseCase() }
    }

    @Test
    fun `ToggleCloudBackup when on calls DisableCloudBackupUseCase`() = runTest {
        isBackupEnabledFlow.value = true
        val vm = createViewModel()

        vm.onIntent(HistoryIntent.ToggleCloudBackup)

        coVerify { disableCloudBackupUseCase() }
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
