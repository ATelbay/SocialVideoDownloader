package com.socialvideodownloader.feature.history.domain

import com.socialvideodownloader.core.domain.model.DownloadRecord
import com.socialvideodownloader.core.domain.model.DownloadStatus
import com.socialvideodownloader.feature.history.testdouble.FakeDownloadRepository
import com.socialvideodownloader.feature.history.testdouble.FakeHistoryFileManager
import com.socialvideodownloader.feature.history.testutil.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MainDispatcherRule::class)
class DeleteHistoryItemUseCaseTest {

    private lateinit var repository: FakeDownloadRepository
    private lateinit var fileManager: FakeHistoryFileManager
    private lateinit var useCase: DeleteHistoryItemUseCase

    @BeforeEach
    fun setUp() {
        repository = FakeDownloadRepository()
        fileManager = FakeHistoryFileManager()
        useCase = DeleteHistoryItemUseCase(repository, fileManager)
    }

    private fun record(
        id: Long = 1L,
        mediaStoreUri: String? = null,
    ) = DownloadRecord(
        id = id,
        sourceUrl = "https://example.com/video",
        videoTitle = "Test Video",
        status = DownloadStatus.COMPLETED,
        createdAt = 1000L,
        mediaStoreUri = mediaStoreUri,
    )

    @Test
    fun `deletes record from repository by id`() = runTest {
        val rec = record(id = 1L)
        repository.insert(rec)

        useCase(itemId = 1L, deleteFile = false)

        assertTrue(repository.deletedRecords.any { it.id == 1L })
    }

    @Test
    fun `does not call deleteFile when deleteFile is false`() = runTest {
        var deleteFileCalled = false
        fileManager.resolveContentUriResult = { "content://media/1" }
        fileManager.deleteFileResult = { deleteFileCalled = true; true }
        val rec = record(id = 1L, mediaStoreUri = "content://media/1")
        repository.insert(rec)

        useCase(itemId = 1L, deleteFile = false)

        assertFalse(deleteFileCalled)
    }

    @Test
    fun `deletes file via HistoryFileManager when deleteFile is true and contentUri is available`() = runTest {
        var deletedUri: String? = null
        fileManager.resolveContentUriResult = { "content://media/1" }
        fileManager.deleteFileResult = { uri -> deletedUri = uri; true }
        val rec = record(id = 1L, mediaStoreUri = "content://media/1")
        repository.insert(rec)

        useCase(itemId = 1L, deleteFile = true)

        assertEquals("content://media/1", deletedUri)
    }

    @Test
    fun `skips file deletion when contentUri cannot be resolved even if deleteFile is true`() = runTest {
        var deleteFileCalled = false
        fileManager.resolveContentUriResult = { null }
        fileManager.deleteFileResult = { deleteFileCalled = true; true }
        val rec = record(id = 1L)
        repository.insert(rec)

        useCase(itemId = 1L, deleteFile = true)

        assertFalse(deleteFileCalled)
    }

    @Test
    fun `deletes Room record even when file deletion fails`() = runTest {
        fileManager.resolveContentUriResult = { "content://media/1" }
        fileManager.deleteFileResult = { false }
        val rec = record(id = 1L, mediaStoreUri = "content://media/1")
        repository.insert(rec)

        useCase(itemId = 1L, deleteFile = true)

        assertTrue(repository.deletedRecords.any { it.id == 1L })
    }

    @Test
    fun `deletes Room record even when file deletion throws`() = runTest {
        fileManager.resolveContentUriResult = { "content://media/1" }
        fileManager.deleteFileResult = { throw RuntimeException("IO error") }
        val rec = record(id = 1L, mediaStoreUri = "content://media/1")
        repository.insert(rec)

        useCase(itemId = 1L, deleteFile = true)

        assertTrue(repository.deletedRecords.any { it.id == 1L })
    }

    @Test
    fun `does nothing when record is not found in repository`() = runTest {
        useCase(itemId = 99L, deleteFile = false)

        assertTrue(repository.deletedRecords.isEmpty())
    }
}
