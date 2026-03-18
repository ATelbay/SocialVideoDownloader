package com.socialvideodownloader.feature.library.ui

import app.cash.turbine.test
import com.socialvideodownloader.core.domain.model.LibraryItem
import com.socialvideodownloader.feature.library.domain.ObserveLibraryItemsUseCase
import com.socialvideodownloader.feature.library.testutil.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class LibraryViewModelTest {

    @RegisterExtension
    val mainDispatcherRule = MainDispatcherRule()

    private val observeLibraryItems = mockk<ObserveLibraryItemsUseCase>()
    private lateinit var viewModel: LibraryViewModel

    private fun libraryItem(
        id: Long,
        title: String = "Test Video",
        contentUri: String = "content://media/$id",
    ) = LibraryItem(
        id = id,
        title = title,
        formatLabel = "1080p",
        thumbnailUrl = null,
        platformName = "YouTube",
        completedAt = 1000L,
        fileSizeBytes = 12345L,
        contentUri = contentUri,
    )

    @BeforeEach
    fun setup() {
        every { observeLibraryItems() } returns flowOf(emptyList())
        viewModel = LibraryViewModel(observeLibraryItems)
    }

    @Test
    fun `initial state is Loading before use case emits`() = runTest {
        every { observeLibraryItems() } returns kotlinx.coroutines.flow.flow { /* never emits */ }
        val vm = LibraryViewModel(observeLibraryItems)
        assertEquals(LibraryUiState.Loading, vm.uiState.value)
    }

    @Test
    fun `when use case emits empty list state becomes Empty`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is LibraryUiState.Empty)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `when use case emits items state becomes Content`() = runTest {
        val items = listOf(libraryItem(1L), libraryItem(2L))
        every { observeLibraryItems() } returns flowOf(items)
        val vm = LibraryViewModel(observeLibraryItems)

        vm.uiState.test {
            val state = awaitItem()
            assertTrue(state is LibraryUiState.Content)
            assertEquals(2, (state as LibraryUiState.Content).items.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `ItemClicked emits OpenContent effect`() = runTest {
        val item = libraryItem(id = 10L, contentUri = "content://media/10")
        every { observeLibraryItems() } returns flowOf(listOf(item))
        val vm = LibraryViewModel(observeLibraryItems)

        vm.uiState.test {
            awaitItem() // Content
            vm.effect.test {
                vm.onIntent(LibraryIntent.ItemClicked(10L))
                val effect = awaitItem()
                assertTrue(effect is LibraryEffect.OpenContent)
                assertEquals("content://media/10", (effect as LibraryEffect.OpenContent).contentUri)
                cancelAndIgnoreRemainingEvents()
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `ItemLongPressed emits ShareContent effect`() = runTest {
        val item = libraryItem(id = 20L, contentUri = "content://media/20")
        every { observeLibraryItems() } returns flowOf(listOf(item))
        val vm = LibraryViewModel(observeLibraryItems)

        vm.uiState.test {
            awaitItem() // Content
            vm.effect.test {
                vm.onIntent(LibraryIntent.ItemLongPressed(20L))
                val effect = awaitItem()
                assertTrue(effect is LibraryEffect.ShareContent)
                assertEquals("content://media/20", (effect as LibraryEffect.ShareContent).contentUri)
                cancelAndIgnoreRemainingEvents()
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `ItemClicked with unknown id emits ShowMessage`() = runTest {
        val item = libraryItem(id = 1L)
        every { observeLibraryItems() } returns flowOf(listOf(item))
        val vm = LibraryViewModel(observeLibraryItems)

        vm.uiState.test {
            awaitItem() // Content
            vm.effect.test {
                vm.onIntent(LibraryIntent.ItemClicked(999L))
                val effect = awaitItem()
                assertTrue(effect is LibraryEffect.ShowMessage)
                cancelAndIgnoreRemainingEvents()
            }
            cancelAndIgnoreRemainingEvents()
        }
    }
}
