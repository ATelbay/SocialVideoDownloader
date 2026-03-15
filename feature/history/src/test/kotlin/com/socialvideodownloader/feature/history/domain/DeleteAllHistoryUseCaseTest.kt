package com.socialvideodownloader.feature.history.domain

import com.socialvideodownloader.core.domain.model.DownloadRecord
import com.socialvideodownloader.core.domain.model.DownloadStatus
import com.socialvideodownloader.feature.history.testdouble.FakeDownloadRepository
import com.socialvideodownloader.feature.history.testdouble.FakeHistoryFileManager
import com.socialvideodownloader.feature.history.testutil.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MainDispatcherRule::class)
class DeleteAllHistoryUseCaseTest {

    private lateinit var repository: FakeDownloadRepository
    private lateinit var fileManager: FakeHistoryFileManager
    private lateinit var useCase: DeleteAllHistoryUseCase

    @BeforeEach
    fun setUp() {
        repository = FakeDownloadRepository()
        fileManager = FakeHistoryFileManager()
        useCase = DeleteAllHistoryUseCase(repository, fileManager)
    }

    private fun record(
        id: Long = 1L,
        mediaStoreUri: String? = null,
        filePath: String? = null,
    ) = DownloadRecord(
        id = id,
        sourceUrl = "https://example.com/video",
        videoTitle = "Video $id",
        thumbnailUrl = null,
        formatLabel = "1080p",
        status = DownloadStatus.COMPLETED,
        createdAt = 1000L,
        fileSizeBytes = 12345L,
        mediaStoreUri = mediaStoreUri,
        filePath = filePath,
    )

    @Test
    fun `calls deleteAll on repository`() = runTest {
        repository.recordsFlow.emit(emptyList())

        useCase(deleteFiles = true)

        assertTrue(repository.deleteAllCalled)
    }

    @Test
    fun `skips file deletion and still calls deleteAll when deleteFiles is false`() = runTest {
        var deleteFileCallCount = 0
        fileManager.deleteFileResult = { _ -> deleteFileCallCount++; true }
        fileManager.resolveContentUriResult = { r -> r.mediaStoreUri }

        repository.recordsFlow.emit(listOf(
            record(id = 1L, mediaStoreUri = "content://media/1"),
            record(id = 2L, mediaStoreUri = "content://media/2"),
        ))

        val result = useCase(deleteFiles = false)

        assertEquals(0, deleteFileCallCount)
        assertTrue(repository.deleteAllCalled)
        assertEquals(0, result.failedFileDeletions)
    }

    @Test
    fun `deletes files for all records that resolve to a contentUri`() = runTest {
        val deletedUris = mutableListOf<String>()
        fileManager.deleteFileResult = { uri -> deletedUris.add(uri); true }

        val rec1 = record(id = 1L, mediaStoreUri = "content://media/1")
        val rec2 = record(id = 2L, mediaStoreUri = "content://media/2")
        val rec3 = record(id = 3L) // no uri
        fileManager.resolveContentUriResult = { r -> r.mediaStoreUri }

        repository.recordsFlow.emit(listOf(rec1, rec2, rec3))

        useCase()

        assertEquals(2, deletedUris.size)
        assertTrue(deletedUris.contains("content://media/1"))
        assertTrue(deletedUris.contains("content://media/2"))
    }

    @Test
    fun `skips file deletion for records that resolve to null contentUri`() = runTest {
        var deleteFileCallCount = 0
        fileManager.deleteFileResult = { _ -> deleteFileCallCount++; true }
        fileManager.resolveContentUriResult = { _ -> null }

        repository.recordsFlow.emit(listOf(record(id = 1L)))

        useCase()

        assertEquals(0, deleteFileCallCount)
    }

    @Test
    fun `still calls repository deleteAll even when file deletion fails`() = runTest {
        fileManager.resolveContentUriResult = { r -> r.mediaStoreUri }
        fileManager.deleteFileResult = { _ -> false }

        repository.recordsFlow.emit(listOf(record(id = 1L, mediaStoreUri = "content://media/1")))

        useCase()

        assertTrue(repository.deleteAllCalled)
    }

    @Test
    fun `reports file cleanup failures as a count of failed deletions`() = runTest {
        fileManager.resolveContentUriResult = { r -> r.mediaStoreUri }
        fileManager.deleteFileResult = { uri -> uri == "content://media/1" }

        val records = listOf(
            record(id = 1L, mediaStoreUri = "content://media/1"),
            record(id = 2L, mediaStoreUri = "content://media/2"),
        )
        repository.recordsFlow.emit(records)

        val result = useCase()

        assertEquals(1, result.failedFileDeletions)
    }

    @Test
    fun `returns zero failedFileDeletions when all files deleted successfully`() = runTest {
        fileManager.resolveContentUriResult = { r -> r.mediaStoreUri }
        fileManager.deleteFileResult = { _ -> true }

        repository.recordsFlow.emit(listOf(
            record(id = 1L, mediaStoreUri = "content://media/1"),
            record(id = 2L, mediaStoreUri = "content://media/2"),
        ))

        val result = useCase()

        assertEquals(0, result.failedFileDeletions)
    }

    @Test
    fun `works correctly with empty history`() = runTest {
        repository.recordsFlow.emit(emptyList())

        val result = useCase()

        assertTrue(repository.deleteAllCalled)
        assertEquals(0, result.failedFileDeletions)
    }
}
