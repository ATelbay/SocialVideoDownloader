package com.socialvideodownloader.feature.history.ui

import app.cash.turbine.test
import com.socialvideodownloader.core.domain.model.SyncStatus
import com.socialvideodownloader.core.domain.repository.BillingRepository
import com.socialvideodownloader.core.domain.sync.BackupPreferences
import com.socialvideodownloader.core.domain.sync.DisableCloudBackupUseCase
import com.socialvideodownloader.core.domain.sync.EnableCloudBackupUseCase
import com.socialvideodownloader.core.domain.sync.ObserveCloudCapacityUseCase
import com.socialvideodownloader.core.domain.sync.CloudAuthService
import com.socialvideodownloader.core.domain.sync.RestoreFromCloudUseCase
import com.socialvideodownloader.core.domain.sync.RestoreResult
import com.socialvideodownloader.core.domain.sync.SyncManager
import com.socialvideodownloader.feature.history.domain.DeleteHistoryItemUseCase
import com.socialvideodownloader.feature.history.domain.ObserveHistoryItemsUseCase
import com.socialvideodownloader.feature.history.testutil.MainDispatcherRule
import android.content.Context
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelRestoreTest {

    @RegisterExtension
    val mainDispatcherRule = MainDispatcherRule()

    private val appContext = mockk<Context>(relaxed = true)
    private val observeHistoryItems = mockk<ObserveHistoryItemsUseCase>()
    private val deleteHistoryItem = mockk<DeleteHistoryItemUseCase>(relaxed = true)
    private val observeCloudCapacity = mockk<ObserveCloudCapacityUseCase>()
    private val billingRepository = mockk<BillingRepository>(relaxed = true)
    private val enableCloudBackupUseCase = mockk<EnableCloudBackupUseCase>(relaxed = true)
    private val disableCloudBackupUseCase = mockk<DisableCloudBackupUseCase>(relaxed = true)
    private val syncManager = mockk<SyncManager>(relaxed = true)
    private val backupPreferences = mockk<BackupPreferences>(relaxed = true)
    private val restoreFromCloudUseCase = mockk<RestoreFromCloudUseCase>()
    private val cloudAuthService = mockk<CloudAuthService>(relaxed = true)

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
        appContext = appContext,
        observeHistoryItems = observeHistoryItems,
        deleteHistoryItem = deleteHistoryItem,
        observeCloudCapacity = observeCloudCapacity,
        billingRepository = billingRepository,
        enableCloudBackupUseCase = enableCloudBackupUseCase,
        disableCloudBackupUseCase = disableCloudBackupUseCase,
        syncManager = syncManager,
        backupPreferences = backupPreferences,
        restoreFromCloudUseCase = restoreFromCloudUseCase,
        cloudAuthService = cloudAuthService,
    )

    @Test
    fun `RestoreFromCloud intent triggers the use case`() = runTest {
        coEvery { restoreFromCloudUseCase(any()) } returns RestoreResult(restored = 3, skipped = 1, failed = 0)
        val vm = createViewModel()

        vm.onIntent(HistoryIntent.RestoreFromCloud)

        coVerify { restoreFromCloudUseCase(any()) }
    }

    @Test
    fun `restore completion state shows correct counts`() = runTest {
        coEvery { restoreFromCloudUseCase(any()) } returns RestoreResult(restored = 5, skipped = 2, failed = 0)
        val vm = createViewModel()

        vm.cloudBackupState.test {
            awaitItem() // initial state

            vm.onIntent(HistoryIntent.RestoreFromCloud)

            val states = mutableListOf<RestoreState>()
            // Collect until Completed
            for (i in 0..10) {
                val state = awaitItem().restoreState
                states.add(state)
                if (state is RestoreState.Completed) break
            }

            val completed = states.filterIsInstance<RestoreState.Completed>().firstOrNull()
            assertTrue(completed != null, "Expected a Completed state")
            assertEquals(5, completed!!.restored)
            assertEquals(2, completed.skipped)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `error state when key is lost`() = runTest {
        coEvery { restoreFromCloudUseCase(any()) } returns RestoreResult(
            restored = 0,
            skipped = 0,
            failed = 0,
            error = "Encryption key no longer available",
        )
        val vm = createViewModel()

        vm.cloudBackupState.test {
            awaitItem() // initial state

            vm.onIntent(HistoryIntent.RestoreFromCloud)

            val states = mutableListOf<RestoreState>()
            for (i in 0..10) {
                val state = awaitItem().restoreState
                states.add(state)
                if (state is RestoreState.Error) break
            }

            val error = states.filterIsInstance<RestoreState.Error>().firstOrNull()
            assertTrue(error != null, "Expected an Error state")
            assertTrue(error!!.message.contains("key", ignoreCase = true))

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `DismissRestoreDialog resets restoreState to Idle`() = runTest {
        coEvery { restoreFromCloudUseCase(any()) } returns RestoreResult(restored = 1, skipped = 0, failed = 0)
        val vm = createViewModel()

        vm.onIntent(HistoryIntent.RestoreFromCloud)
        vm.onIntent(HistoryIntent.DismissRestoreDialog)

        val state = vm.cloudBackupState.value.restoreState
        assertEquals(RestoreState.Idle, state)
    }
}
