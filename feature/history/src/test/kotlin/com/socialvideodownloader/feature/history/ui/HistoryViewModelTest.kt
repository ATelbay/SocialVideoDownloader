package com.socialvideodownloader.feature.history.ui

import app.cash.turbine.test
import com.socialvideodownloader.core.domain.model.DownloadStatus
import com.socialvideodownloader.core.domain.model.HistoryItem
import com.socialvideodownloader.core.domain.model.SyncStatus
import com.socialvideodownloader.core.domain.repository.BillingRepository
import com.socialvideodownloader.core.domain.sync.BackupPreferences
import com.socialvideodownloader.core.domain.sync.DisableCloudBackupUseCase
import com.socialvideodownloader.core.domain.sync.EnableCloudBackupUseCase
import com.socialvideodownloader.core.domain.sync.ObserveCloudCapacityUseCase
import com.socialvideodownloader.core.domain.sync.RestoreFromCloudUseCase
import com.socialvideodownloader.core.domain.sync.SyncManager
import com.socialvideodownloader.feature.history.domain.DeleteHistoryItemUseCase
import com.socialvideodownloader.feature.history.domain.ObserveHistoryItemsUseCase
import com.socialvideodownloader.feature.history.testutil.MainDispatcherRule
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class HistoryViewModelTest {

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
    private val restoreFromCloudUseCase = mockk<RestoreFromCloudUseCase>(relaxed = true)
    private lateinit var viewModel: HistoryViewModel

    private lateinit var testItems: List<HistoryItem>

    @BeforeEach
    fun setup() {
        testItems = listOf(
            historyItem(id = 1L, title = "Kotlin Tutorial", isFileAccessible = true),
            historyItem(id = 2L, title = "Android Compose Guide"),
            historyItem(id = 3L, title = "kotlin advanced"),
        )
        every { observeHistoryItems() } returns flowOf(testItems)
        every { observeCloudCapacity() } returns flowOf()
        every { backupPreferences.observeIsBackupEnabled() } returns flowOf(false)
        every { syncManager.observeSyncStatus() } returns flowOf(SyncStatus.Idle)
        viewModel = HistoryViewModel(
            observeHistoryItems = observeHistoryItems,
            deleteHistoryItem = deleteHistoryItem,
            observeCloudCapacity = observeCloudCapacity,
            billingRepository = billingRepository,
            enableCloudBackupUseCase = enableCloudBackupUseCase,
            disableCloudBackupUseCase = disableCloudBackupUseCase,
            syncManager = syncManager,
            backupPreferences = backupPreferences,
            restoreFromCloudUseCase = restoreFromCloudUseCase,
        )
    }

    @Test
    fun `initial state is Loading before use case emits`() = runTest {
        // The initial value before any emission is Loading
        // With UnconfinedTestDispatcher the flow starts immediately, so we verify the sealed type
        // by recreating with a never-emitting flow
        every { observeHistoryItems() } returns kotlinx.coroutines.flow.flow { /* never emits */ }
        val vm = HistoryViewModel(
                observeHistoryItems = observeHistoryItems,
                deleteHistoryItem = deleteHistoryItem,
                observeCloudCapacity = observeCloudCapacity,
                billingRepository = billingRepository,
                enableCloudBackupUseCase = enableCloudBackupUseCase,
                disableCloudBackupUseCase = disableCloudBackupUseCase,
                syncManager = syncManager,
                backupPreferences = backupPreferences,
                restoreFromCloudUseCase = restoreFromCloudUseCase,
            )
        assertEquals(HistoryUiState.Loading, vm.uiState.value)
    }

    @Test
    fun `when use case emits items state becomes Content with all items`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is HistoryUiState.Content)
            assertEquals(testItems.size, (state as HistoryUiState.Content).items.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `when use case emits empty list state becomes Empty with isFiltering false`() = runTest {
        every { observeHistoryItems() } returns flowOf(emptyList())
        val vm = HistoryViewModel(
                observeHistoryItems = observeHistoryItems,
                deleteHistoryItem = deleteHistoryItem,
                observeCloudCapacity = observeCloudCapacity,
                billingRepository = billingRepository,
                enableCloudBackupUseCase = enableCloudBackupUseCase,
                disableCloudBackupUseCase = disableCloudBackupUseCase,
                syncManager = syncManager,
                backupPreferences = backupPreferences,
                restoreFromCloudUseCase = restoreFromCloudUseCase,
            )
        vm.uiState.test {
            val state = awaitItem()
            assertTrue(state is HistoryUiState.Empty)
            state as HistoryUiState.Empty
            assertEquals("", state.query)
            assertFalse(state.isFiltering)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `SearchQueryChanged with matching text produces Content with filtered items`() = runTest {
        viewModel.uiState.test {
            awaitItem() // initial Content

            viewModel.onIntent(HistoryIntent.SearchQueryChanged("kotlin"))

            val filtered = awaitItem()
            assertTrue(filtered is HistoryUiState.Content)
            filtered as HistoryUiState.Content
            assertEquals("kotlin", filtered.query)
            assertEquals(2, filtered.items.size)
            assertTrue(filtered.items.all { it.title.contains("kotlin", ignoreCase = true) })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `SearchQueryChanged with non-matching text produces Empty with isFiltering true`() = runTest {
        viewModel.uiState.test {
            awaitItem() // initial Content

            viewModel.onIntent(HistoryIntent.SearchQueryChanged("zzz-no-match"))

            val state = awaitItem()
            assertTrue(state is HistoryUiState.Empty)
            state as HistoryUiState.Empty
            assertEquals("zzz-no-match", state.query)
            assertTrue(state.isFiltering)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `clearing search query returns to full Content list`() = runTest {
        viewModel.uiState.test {
            awaitItem() // initial Content

            viewModel.onIntent(HistoryIntent.SearchQueryChanged("kotlin"))
            awaitItem() // filtered Content

            viewModel.onIntent(HistoryIntent.SearchQueryChanged(""))
            val state = awaitItem()
            assertTrue(state is HistoryUiState.Content)
            assertEquals(testItems.size, (state as HistoryUiState.Content).items.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `search is case-insensitive substring match on title`() = runTest {
        viewModel.uiState.test {
            awaitItem() // initial Content

            viewModel.onIntent(HistoryIntent.SearchQueryChanged("KOTLIN"))

            val state = awaitItem()
            assertTrue(state is HistoryUiState.Content)
            state as HistoryUiState.Content
            assertEquals(2, state.items.size)
            assertTrue(state.items.any { it.title == "Kotlin Tutorial" })
            assertTrue(state.items.any { it.title == "kotlin advanced" })
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- T024: open/share/menu tests ---

    @Test
    fun `historyItemClicked on completed accessible item emits OpenContent`() = runTest {
        val accessibleItem = historyItem(
            id = 10L,
            title = "Accessible Video",
            status = DownloadStatus.COMPLETED,
            isFileAccessible = true,
            contentUri = "content://media/external/video/10",
        )
        every { observeHistoryItems() } returns flowOf(listOf(accessibleItem))
        val vm = HistoryViewModel(
                observeHistoryItems = observeHistoryItems,
                deleteHistoryItem = deleteHistoryItem,
                observeCloudCapacity = observeCloudCapacity,
                billingRepository = billingRepository,
                enableCloudBackupUseCase = enableCloudBackupUseCase,
                disableCloudBackupUseCase = disableCloudBackupUseCase,
                syncManager = syncManager,
                backupPreferences = backupPreferences,
                restoreFromCloudUseCase = restoreFromCloudUseCase,
            )

        vm.uiState.test {
            awaitItem() // subscribe so _allItems is populated
            vm.effect.test {
                vm.onIntent(HistoryIntent.HistoryItemClicked(10L))
                val effect = awaitItem()
                assertTrue(effect is HistoryEffect.OpenContent)
                assertEquals("content://media/external/video/10", (effect as HistoryEffect.OpenContent).contentUri)
                cancelAndIgnoreRemainingEvents()
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `historyItemClicked on completed inaccessible item emits ShowMessage`() = runTest {
        val inaccessibleItem = historyItem(
            id = 11L,
            title = "Inaccessible Video",
            status = DownloadStatus.COMPLETED,
            isFileAccessible = false,
        )
        every { observeHistoryItems() } returns flowOf(listOf(inaccessibleItem))
        val vm = HistoryViewModel(
                observeHistoryItems = observeHistoryItems,
                deleteHistoryItem = deleteHistoryItem,
                observeCloudCapacity = observeCloudCapacity,
                billingRepository = billingRepository,
                enableCloudBackupUseCase = enableCloudBackupUseCase,
                disableCloudBackupUseCase = disableCloudBackupUseCase,
                syncManager = syncManager,
                backupPreferences = backupPreferences,
                restoreFromCloudUseCase = restoreFromCloudUseCase,
            )

        vm.uiState.test {
            awaitItem()
            vm.effect.test {
                vm.onIntent(HistoryIntent.HistoryItemClicked(11L))
                val effect = awaitItem()
                assertTrue(effect is HistoryEffect.ShowMessage)
                cancelAndIgnoreRemainingEvents()
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `historyItemClicked on failed item emits RetryDownload`() = runTest {
        val failedItem = historyItem(
            id = 12L,
            title = "Failed Video",
            status = DownloadStatus.FAILED,
            isFileAccessible = false,
        )
        every { observeHistoryItems() } returns flowOf(listOf(failedItem))
        val vm = HistoryViewModel(
                observeHistoryItems = observeHistoryItems,
                deleteHistoryItem = deleteHistoryItem,
                observeCloudCapacity = observeCloudCapacity,
                billingRepository = billingRepository,
                enableCloudBackupUseCase = enableCloudBackupUseCase,
                disableCloudBackupUseCase = disableCloudBackupUseCase,
                syncManager = syncManager,
                backupPreferences = backupPreferences,
                restoreFromCloudUseCase = restoreFromCloudUseCase,
            )

        vm.uiState.test {
            awaitItem()
            vm.effect.test {
                vm.onIntent(HistoryIntent.HistoryItemClicked(12L))
                val effect = awaitItem()
                assertTrue(effect is HistoryEffect.RetryDownload)
                assertEquals("https://example.com/video", (effect as HistoryEffect.RetryDownload).sourceUrl)
                cancelAndIgnoreRemainingEvents()
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `shareClicked on accessible item emits ShareContent`() = runTest {
        val accessibleItem = historyItem(
            id = 20L,
            title = "Share Video",
            status = DownloadStatus.COMPLETED,
            isFileAccessible = true,
            contentUri = "content://media/external/video/20",
        )
        every { observeHistoryItems() } returns flowOf(listOf(accessibleItem))
        val vm = HistoryViewModel(
                observeHistoryItems = observeHistoryItems,
                deleteHistoryItem = deleteHistoryItem,
                observeCloudCapacity = observeCloudCapacity,
                billingRepository = billingRepository,
                enableCloudBackupUseCase = enableCloudBackupUseCase,
                disableCloudBackupUseCase = disableCloudBackupUseCase,
                syncManager = syncManager,
                backupPreferences = backupPreferences,
                restoreFromCloudUseCase = restoreFromCloudUseCase,
            )

        vm.uiState.test {
            awaitItem()
            vm.effect.test {
                vm.onIntent(HistoryIntent.ShareClicked(20L))
                val effect = awaitItem()
                assertTrue(effect is HistoryEffect.ShareContent)
                assertEquals("content://media/external/video/20", (effect as HistoryEffect.ShareContent).contentUri)
                cancelAndIgnoreRemainingEvents()
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `shareClicked on inaccessible item emits ShowMessage`() = runTest {
        val inaccessibleItem = historyItem(
            id = 21L,
            title = "Inaccessible Share",
            status = DownloadStatus.COMPLETED,
            isFileAccessible = false,
        )
        every { observeHistoryItems() } returns flowOf(listOf(inaccessibleItem))
        val vm = HistoryViewModel(
                observeHistoryItems = observeHistoryItems,
                deleteHistoryItem = deleteHistoryItem,
                observeCloudCapacity = observeCloudCapacity,
                billingRepository = billingRepository,
                enableCloudBackupUseCase = enableCloudBackupUseCase,
                disableCloudBackupUseCase = disableCloudBackupUseCase,
                syncManager = syncManager,
                backupPreferences = backupPreferences,
                restoreFromCloudUseCase = restoreFromCloudUseCase,
            )

        vm.uiState.test {
            awaitItem()
            vm.effect.test {
                vm.onIntent(HistoryIntent.ShareClicked(21L))
                val effect = awaitItem()
                assertTrue(effect is HistoryEffect.ShowMessage)
                cancelAndIgnoreRemainingEvents()
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `historyItemLongPressed sets openMenuItemId`() = runTest {
        viewModel.uiState.test {
            awaitItem() // initial Content

            viewModel.onIntent(HistoryIntent.HistoryItemLongPressed(1L))

            val state = awaitItem()
            assertTrue(state is HistoryUiState.Content)
            assertEquals(1L, (state as HistoryUiState.Content).openMenuItemId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `dismissItemMenu clears openMenuItemId`() = runTest {
        viewModel.uiState.test {
            awaitItem() // initial Content

            viewModel.onIntent(HistoryIntent.HistoryItemLongPressed(1L))
            awaitItem() // state with openMenuItemId = 1

            viewModel.onIntent(HistoryIntent.DismissItemMenu)
            val state = awaitItem()
            assertTrue(state is HistoryUiState.Content)
            assertNull((state as HistoryUiState.Content).openMenuItemId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- T033: delete flow tests ---

    @Test
    fun `DeleteItemClicked shows confirmation dialog for single item`() = runTest {
        viewModel.uiState.test {
            awaitItem() // initial Content

            viewModel.onIntent(HistoryIntent.DeleteItemClicked(1L))

            val state = awaitItem()
            assertTrue(state is HistoryUiState.Content)
            state as HistoryUiState.Content
            assertNotNull(state.deleteConfirmation)
            val confirmation = state.deleteConfirmation!!
            assertTrue(confirmation.target is DeleteTarget.Single)
            assertEquals(1L, (confirmation.target as DeleteTarget.Single).itemId)
            assertEquals(1, confirmation.affectedCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `DeleteItemClicked sets hasAnyAccessibleFile based on item accessibility`() = runTest {
        viewModel.uiState.test {
            awaitItem() // initial Content

            // item 1 is accessible
            viewModel.onIntent(HistoryIntent.DeleteItemClicked(1L))
            val withAccessible = awaitItem() as HistoryUiState.Content
            assertTrue(withAccessible.deleteConfirmation!!.hasAnyAccessibleFile)

            viewModel.onIntent(HistoryIntent.DismissDeletionDialog)
            awaitItem() // dismissed

            // item 2 is not accessible
            viewModel.onIntent(HistoryIntent.DeleteItemClicked(2L))
            val withInaccessible = awaitItem() as HistoryUiState.Content
            assertFalse(withInaccessible.deleteConfirmation!!.hasAnyAccessibleFile)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `DismissDeletionDialog clears deleteConfirmation`() = runTest {
        viewModel.uiState.test {
            awaitItem() // initial Content

            viewModel.onIntent(HistoryIntent.DeleteItemClicked(1L))
            awaitItem() // confirmation shown

            viewModel.onIntent(HistoryIntent.DismissDeletionDialog)

            val state = awaitItem() as HistoryUiState.Content
            assertNull(state.deleteConfirmation)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `DeleteFilesSelectionChanged toggles deleteFilesSelected in confirmation`() = runTest {
        viewModel.uiState.test {
            awaitItem() // initial Content

            viewModel.onIntent(HistoryIntent.DeleteItemClicked(1L))
            val initial = awaitItem() as HistoryUiState.Content
            assertFalse(initial.deleteConfirmation!!.deleteFilesSelected)

            viewModel.onIntent(HistoryIntent.DeleteFilesSelectionChanged(true))
            val toggled = awaitItem() as HistoryUiState.Content
            assertTrue(toggled.deleteConfirmation!!.deleteFilesSelected)

            viewModel.onIntent(HistoryIntent.DeleteFilesSelectionChanged(false))
            val untoggled = awaitItem() as HistoryUiState.Content
            assertFalse(untoggled.deleteConfirmation!!.deleteFilesSelected)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `ConfirmDeletion for single item calls DeleteHistoryItemUseCase with correct args`() = runTest {
        viewModel.uiState.test {
            awaitItem() // initial Content

            viewModel.onIntent(HistoryIntent.DeleteItemClicked(1L))
            awaitItem() // confirmation shown

            viewModel.onIntent(HistoryIntent.DeleteFilesSelectionChanged(true))
            awaitItem() // deleteFilesSelected toggled

            viewModel.onIntent(HistoryIntent.ConfirmDeletion)

            cancelAndIgnoreRemainingEvents()
        }

        coVerify { deleteHistoryItem(itemId = 1L, deleteFile = true) }
    }

    @Test
    fun `ConfirmDeletion for single item clears confirmation after deletion`() = runTest {
        viewModel.uiState.test {
            awaitItem() // initial Content

            viewModel.onIntent(HistoryIntent.DeleteItemClicked(1L))
            awaitItem() // confirmation shown

            viewModel.onIntent(HistoryIntent.ConfirmDeletion)

            // After deletion confirmation is cleared (isDeleting may briefly appear)
            val finalState = awaitItem()
            assertTrue(finalState is HistoryUiState.Content)
            assertNull((finalState as HistoryUiState.Content).deleteConfirmation)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- helpers ---

    private fun historyItem(
        id: Long,
        title: String,
        status: DownloadStatus = DownloadStatus.COMPLETED,
        isFileAccessible: Boolean = false,
        contentUri: String? = null,
    ) = HistoryItem(
        id = id,
        title = title,
        formatLabel = null,
        thumbnailUrl = null,
        sourceUrl = "https://example.com/video",
        status = status,
        createdAt = 0L,
        fileSizeBytes = null,
        contentUri = contentUri,
        isFileAccessible = isFileAccessible,
    )
}
