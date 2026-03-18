package com.socialvideodownloader.feature.library.domain

import app.cash.turbine.test
import com.socialvideodownloader.core.domain.model.DownloadRecord
import com.socialvideodownloader.core.domain.model.DownloadStatus
import com.socialvideodownloader.feature.library.testdouble.FakeDownloadRepository
import com.socialvideodownloader.feature.library.testdouble.FakeFileAccessManager
import com.socialvideodownloader.feature.library.testutil.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MainDispatcherRule::class)
class ObserveLibraryItemsUseCaseTest {

    private lateinit var repository: FakeDownloadRepository
    private lateinit var fileManager: FakeFileAccessManager
    private lateinit var useCase: ObserveLibraryItemsUseCase

    @BeforeEach
    fun setUp() {
        repository = FakeDownloadRepository()
        fileManager = FakeFileAccessManager()
        useCase = ObserveLibraryItemsUseCase(repository, fileManager)
    }

    private fun completedRecord(
        id: Long = 1L,
        title: String = "Test Video",
        sourceUrl: String = "https://youtube.com/watch?v=abc",
        fileSizeBytes: Long? = 12345L,
        completedAt: Long? = 2000L,
    ) = DownloadRecord(
        id = id,
        sourceUrl = sourceUrl,
        videoTitle = title,
        thumbnailUrl = "https://example.com/thumb.jpg",
        formatLabel = "1080p",
        status = DownloadStatus.COMPLETED,
        createdAt = 1000L,
        completedAt = completedAt,
        fileSizeBytes = fileSizeBytes,
    )

    @Test
    fun `empty list emits empty flow`() = runTest {
        useCase().test {
            repository.recordsFlow.emit(emptyList())
            val items = awaitItem()
            assertTrue(items.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `filters out records where file is not accessible`() = runTest {
        val accessible = completedRecord(id = 1L, title = "Accessible")
        val inaccessible = completedRecord(id = 2L, title = "Inaccessible")
        fileManager.resolveContentUriResult = { record ->
            if (record.id == 1L) "content://media/1" else "content://media/2"
        }
        fileManager.isFileAccessibleResult = { uri -> uri == "content://media/1" }

        useCase().test {
            repository.recordsFlow.emit(listOf(accessible, inaccessible))
            val items = awaitItem()
            assertEquals(1, items.size)
            assertEquals(1L, items[0].id)
            assertEquals("Accessible", items[0].title)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `filters out records where contentUri is null`() = runTest {
        val record = completedRecord(id = 1L)
        fileManager.resolveContentUriResult = { null }

        useCase().test {
            repository.recordsFlow.emit(listOf(record))
            val items = awaitItem()
            assertTrue(items.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `maps DownloadRecord correctly to LibraryListItem`() = runTest {
        val record = completedRecord(
            id = 42L,
            title = "My Video",
            sourceUrl = "https://youtube.com/watch?v=xyz",
            fileSizeBytes = 99999L,
            completedAt = 5000L,
        )
        fileManager.resolveContentUriResult = { "content://media/42" }
        fileManager.isFileAccessibleResult = { true }

        useCase().test {
            repository.recordsFlow.emit(listOf(record))
            val items = awaitItem()
            assertEquals(1, items.size)
            val item = items[0]
            assertEquals(42L, item.id)
            assertEquals("My Video", item.title)
            assertEquals("1080p", item.formatLabel)
            assertEquals("https://example.com/thumb.jpg", item.thumbnailUrl)
            assertEquals("YouTube", item.platformName)
            assertEquals(5000L, item.completedAt)
            assertEquals(99999L, item.fileSizeBytes)
            assertEquals("content://media/42", item.contentUri)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `uses createdAt as fallback when completedAt is null`() = runTest {
        val record = completedRecord(completedAt = null)
        fileManager.resolveContentUriResult = { "content://media/1" }
        fileManager.isFileAccessibleResult = { true }

        useCase().test {
            repository.recordsFlow.emit(listOf(record))
            val items = awaitItem()
            assertEquals(1000L, items[0].completedAt)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `only returns completed downloads`() = runTest {
        val completed = completedRecord(id = 1L)
        val failed = DownloadRecord(
            id = 2L,
            sourceUrl = "https://youtube.com/x",
            videoTitle = "Failed",
            status = DownloadStatus.FAILED,
            createdAt = 1000L,
        )
        fileManager.resolveContentUriResult = { "content://media/${it.id}" }
        fileManager.isFileAccessibleResult = { true }

        useCase().test {
            repository.recordsFlow.emit(listOf(completed, failed))
            val items = awaitItem()
            assertEquals(1, items.size)
            assertEquals(1L, items[0].id)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
