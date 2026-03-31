package com.socialvideodownloader.feature.library.ui

import app.cash.turbine.test
import com.socialvideodownloader.core.domain.model.DownloadRecord
import com.socialvideodownloader.core.domain.model.DownloadStatus
import com.socialvideodownloader.feature.library.testdouble.FakeDownloadRepository
import com.socialvideodownloader.feature.library.testdouble.FakeFileAccessManager
import com.socialvideodownloader.feature.library.testutil.MainDispatcherRule
import com.socialvideodownloader.shared.feature.library.LibraryEffect.OpenContent
import com.socialvideodownloader.shared.feature.library.LibraryEffect.ShareContent
import com.socialvideodownloader.shared.feature.library.LibraryEffect.ShowMessage
import com.socialvideodownloader.shared.feature.library.LibraryIntent.ItemClicked
import com.socialvideodownloader.shared.feature.library.LibraryIntent.ItemLongPressed
import com.socialvideodownloader.shared.feature.library.LibraryUiState.Content
import com.socialvideodownloader.shared.feature.library.LibraryUiState.Empty
import com.socialvideodownloader.shared.feature.library.LibraryUiState.Loading
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class LibraryViewModelTest {

    @RegisterExtension
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeDownloadRepository()
    private val fileManager = FakeFileAccessManager()
    private lateinit var viewModel: LibraryViewModel

    @BeforeEach
    fun setup() {
        viewModel = LibraryViewModel(repository, fileManager)
    }

    private suspend fun emitCompleted(records: List<DownloadRecord>) {
        repository.recordsFlow.emit(records)
    }

    private fun completedRecord(
        id: Long,
        title: String = "Test Video",
        contentUri: String = "content://media/$id",
    ) = DownloadRecord(
        id = id,
        videoTitle = title,
        sourceUrl = "https://youtube.com/watch?v=$id",
        status = DownloadStatus.COMPLETED,
        createdAt = 1000L,
        fileSizeBytes = 12345L,
        mediaStoreUri = contentUri,
    )

    @Test
    fun `initial state is Loading before repository emits`() = runTest {
        val vm = LibraryViewModel(repository, fileManager)
        assertEquals(Loading, vm.uiState.value)
    }

    @Test
    fun `when repository emits no completed records state becomes Empty`() = runTest {
        viewModel.uiState.test {
            awaitItem() // Loading
            emitCompleted(emptyList())
            val state = awaitItem()
            assertTrue(state is Empty)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `when repository emits completed accessible records state becomes Content`() = runTest {
        val records = listOf(
            completedRecord(1L, contentUri = "content://media/1"),
            completedRecord(2L, contentUri = "content://media/2"),
        )
        fileManager.resolveContentUriResult = { record -> record.mediaStoreUri }
        fileManager.isFileAccessibleResult = { true }

        viewModel.uiState.test {
            awaitItem() // Loading
            emitCompleted(records)
            val state = awaitItem()
            assertTrue(state is Content)
            assertEquals(2, (state as Content).items.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `inaccessible files are excluded from Content`() = runTest {
        val records = listOf(
            completedRecord(1L, contentUri = "content://media/1"),
            completedRecord(2L, contentUri = "content://media/2"),
        )
        fileManager.resolveContentUriResult = { record -> record.mediaStoreUri }
        // Only record 1 is accessible
        fileManager.isFileAccessibleResult = { uri -> uri == "content://media/1" }

        viewModel.uiState.test {
            awaitItem() // Loading
            emitCompleted(records)
            val state = awaitItem()
            assertTrue(state is Content)
            assertEquals(1, (state as Content).items.size)
            assertEquals(1L, state.items.first().id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `ItemClicked emits OpenContent effect`() = runTest {
        val record = completedRecord(id = 10L, contentUri = "content://media/10")
        fileManager.resolveContentUriResult = { it.mediaStoreUri }
        fileManager.isFileAccessibleResult = { true }

        viewModel.uiState.test {
            awaitItem() // Loading
            emitCompleted(listOf(record))
            awaitItem() // Content
            viewModel.effect.test {
                viewModel.onIntent(ItemClicked(10L))
                val effect = awaitItem()
                assertTrue(effect is OpenContent)
                assertEquals("content://media/10", (effect as OpenContent).contentUri)
                cancelAndIgnoreRemainingEvents()
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `ItemLongPressed emits ShareContent effect`() = runTest {
        val record = completedRecord(id = 20L, contentUri = "content://media/20")
        fileManager.resolveContentUriResult = { it.mediaStoreUri }
        fileManager.isFileAccessibleResult = { true }

        viewModel.uiState.test {
            awaitItem() // Loading
            emitCompleted(listOf(record))
            awaitItem() // Content
            viewModel.effect.test {
                viewModel.onIntent(ItemLongPressed(20L))
                val effect = awaitItem()
                assertTrue(effect is ShareContent)
                assertEquals("content://media/20", (effect as ShareContent).contentUri)
                cancelAndIgnoreRemainingEvents()
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `ItemClicked with unknown id emits ShowMessage`() = runTest {
        val record = completedRecord(id = 1L, contentUri = "content://media/1")
        fileManager.resolveContentUriResult = { it.mediaStoreUri }
        fileManager.isFileAccessibleResult = { true }

        viewModel.uiState.test {
            awaitItem() // Loading
            emitCompleted(listOf(record))
            awaitItem() // Content
            viewModel.effect.test {
                viewModel.onIntent(ItemClicked(999L))
                val effect = awaitItem()
                assertTrue(effect is ShowMessage)
                cancelAndIgnoreRemainingEvents()
            }
            cancelAndIgnoreRemainingEvents()
        }
    }
}
