package com.socialvideodownloader.feature.history.domain

import app.cash.turbine.test
import com.socialvideodownloader.core.domain.model.DownloadRecord
import com.socialvideodownloader.core.domain.model.DownloadStatus
import com.socialvideodownloader.feature.history.testdouble.FakeDownloadRepository
import com.socialvideodownloader.feature.history.testdouble.FakeHistoryFileManager
import com.socialvideodownloader.feature.history.testutil.MainDispatcherRule
import com.socialvideodownloader.core.domain.model.HistoryItem
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MainDispatcherRule::class)
class ObserveHistoryItemsUseCaseTest {

    private lateinit var repository: FakeDownloadRepository
    private lateinit var fileManager: FakeHistoryFileManager
    private lateinit var useCase: ObserveHistoryItemsUseCase

    @BeforeEach
    fun setUp() {
        repository = FakeDownloadRepository()
        fileManager = FakeHistoryFileManager()
        useCase = ObserveHistoryItemsUseCase(repository, fileManager)
    }

    private fun record(
        id: Long = 1L,
        title: String = "Test Video",
        formatLabel: String = "1080p",
        thumbnailUrl: String? = "https://example.com/thumb.jpg",
        status: DownloadStatus = DownloadStatus.COMPLETED,
        createdAt: Long = 1000L,
        fileSizeBytes: Long? = 12345L,
    ) = DownloadRecord(
        id = id,
        sourceUrl = "https://example.com/video",
        videoTitle = title,
        thumbnailUrl = thumbnailUrl,
        formatLabel = formatLabel,
        status = status,
        createdAt = createdAt,
        fileSizeBytes = fileSizeBytes,
    )

    @Test
    fun `maps DownloadRecord list to HistoryListItem list with all fields correctly mapped`() = runTest {
        val rec = record()
        fileManager.resolveContentUriResult = { "content://media/external/video/1" }
        fileManager.isFileAccessibleResult = { true }

        useCase().test {
            repository.recordsFlow.emit(listOf(rec))
            val items = awaitItem()
            assertEquals(1, items.size)
            val item = items[0]
            assertEquals(rec.id, item.id)
            assertEquals(rec.videoTitle, item.title)
            assertEquals(rec.formatLabel, item.formatLabel)
            assertEquals(rec.thumbnailUrl, item.thumbnailUrl)
            assertEquals(rec.status, item.status)
            assertEquals(rec.createdAt, item.createdAt)
            assertEquals(rec.fileSizeBytes, item.fileSizeBytes)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `resolves contentUri via HistoryFileManager resolveContentUri for each record`() = runTest {
        val rec1 = record(id = 1L)
        val rec2 = record(id = 2L)
        val uriMap = mapOf(1L to "content://media/1", 2L to "content://media/2")
        fileManager.resolveContentUriResult = { r -> uriMap[r.id] }
        fileManager.isFileAccessibleResult = { true }

        useCase().test {
            repository.recordsFlow.emit(listOf(rec1, rec2))
            val items = awaitItem()
            assertEquals("content://media/1", items[0].contentUri)
            assertEquals("content://media/2", items[1].contentUri)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `sets isFileAccessible=true when contentUri is non-null and fileManager isFileAccessible returns true`() = runTest {
        val rec = record()
        fileManager.resolveContentUriResult = { "content://media/external/video/1" }
        fileManager.isFileAccessibleResult = { true }

        useCase().test {
            repository.recordsFlow.emit(listOf(rec))
            val item = awaitItem()[0]
            assertTrue(item.isFileAccessible)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `sets isFileAccessible=false when contentUri is null`() = runTest {
        val rec = record()
        fileManager.resolveContentUriResult = { null }
        fileManager.isFileAccessibleResult = { true }

        useCase().test {
            repository.recordsFlow.emit(listOf(rec))
            val item = awaitItem()[0]
            assertFalse(item.isFileAccessible)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `sets isFileAccessible=false when fileManager isFileAccessible returns false`() = runTest {
        val rec = record()
        fileManager.resolveContentUriResult = { "content://media/external/video/1" }
        fileManager.isFileAccessibleResult = { false }

        useCase().test {
            repository.recordsFlow.emit(listOf(rec))
            val item = awaitItem()[0]
            assertFalse(item.isFileAccessible)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits updated list when repository flow emits new data`() = runTest {
        fileManager.resolveContentUriResult = { null }
        fileManager.isFileAccessibleResult = { false }

        useCase().test {
            repository.recordsFlow.emit(listOf(record(id = 1L, title = "First")))
            val first = awaitItem()
            assertEquals(1, first.size)
            assertEquals("First", first[0].title)

            repository.recordsFlow.emit(listOf(record(id = 1L, title = "First"), record(id = 2L, title = "Second")))
            val second = awaitItem()
            assertEquals(2, second.size)
            assertEquals("Second", second[1].title)

            cancelAndIgnoreRemainingEvents()
        }
    }
}
