package com.socialvideodownloader.core.domain.usecase

import com.socialvideodownloader.core.domain.file.FileAccessManager
import com.socialvideodownloader.core.domain.model.DownloadRecord
import com.socialvideodownloader.core.domain.model.DownloadStatus
import com.socialvideodownloader.core.domain.repository.DownloadRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FindExistingDownloadUseCaseTest {

    private lateinit var repository: DownloadRepository
    private lateinit var fileAccessManager: FileAccessManager
    private lateinit var useCase: FindExistingDownloadUseCase

    private val completedRecord = DownloadRecord(
        id = 42L,
        sourceUrl = "https://youtube.com/watch?v=abc123&si=tracker",
        videoTitle = "Test Video",
        thumbnailUrl = "https://img.youtube.com/thumb.jpg",
        formatLabel = "1080p",
        filePath = null,
        mediaStoreUri = "content://media/downloads/42",
        status = DownloadStatus.COMPLETED,
        createdAt = 1000L,
        completedAt = 2000L,
        fileSizeBytes = 50_000_000L,
    )

    @BeforeEach
    fun setup() {
        repository = mockk()
        fileAccessManager = mockk()
        useCase = FindExistingDownloadUseCase(repository, fileAccessManager)
    }

    @Test
    fun `returns null when no completed downloads match`() = runTest {
        coEvery { repository.getCompletedSnapshot() } returns emptyList()

        val result = useCase("https://youtube.com/watch?v=abc123")

        assertNull(result)
    }

    @Test
    fun `returns null when url does not match any completed record`() = runTest {
        coEvery { repository.getCompletedSnapshot() } returns listOf(completedRecord)

        val result = useCase("https://youtube.com/watch?v=different")

        assertNull(result)
    }

    @Test
    fun `returns null when matched record has no content uri`() = runTest {
        coEvery { repository.getCompletedSnapshot() } returns listOf(completedRecord)
        coEvery { fileAccessManager.resolveContentUri(completedRecord) } returns null

        val result = useCase("https://youtube.com/watch?v=abc123")

        assertNull(result)
    }

    @Test
    fun `returns null when file is not accessible`() = runTest {
        coEvery { repository.getCompletedSnapshot() } returns listOf(completedRecord)
        coEvery { fileAccessManager.resolveContentUri(completedRecord) } returns "content://media/downloads/42"
        coEvery { fileAccessManager.isFileAccessible("content://media/downloads/42") } returns false

        val result = useCase("https://youtube.com/watch?v=abc123")

        assertNull(result)
    }

    @Test
    fun `returns ExistingDownload when url matches and file is accessible`() = runTest {
        coEvery { repository.getCompletedSnapshot() } returns listOf(completedRecord)
        coEvery { fileAccessManager.resolveContentUri(completedRecord) } returns "content://media/downloads/42"
        coEvery { fileAccessManager.isFileAccessible("content://media/downloads/42") } returns true

        val result = useCase("https://youtube.com/watch?v=abc123")

        assertEquals(42L, result?.recordId)
        assertEquals("Test Video", result?.videoTitle)
        assertEquals("1080p", result?.formatLabel)
        assertEquals("https://img.youtube.com/thumb.jpg", result?.thumbnailUrl)
        assertEquals("content://media/downloads/42", result?.contentUri)
        assertEquals(2000L, result?.completedAt)
        assertEquals(50_000_000L, result?.fileSizeBytes)
    }

    @Test
    fun `matches url despite tracking params in stored record`() = runTest {
        coEvery { repository.getCompletedSnapshot() } returns listOf(completedRecord)
        coEvery { fileAccessManager.resolveContentUri(completedRecord) } returns "content://media/downloads/42"
        coEvery { fileAccessManager.isFileAccessible("content://media/downloads/42") } returns true

        // Input URL without tracking param should match the stored URL that had si= param
        val result = useCase("https://youtube.com/watch?v=abc123")

        assertEquals(42L, result?.recordId)
    }

    @Test
    fun `matches youtu_be short url against stored full url`() = runTest {
        coEvery { repository.getCompletedSnapshot() } returns listOf(completedRecord)
        coEvery { fileAccessManager.resolveContentUri(completedRecord) } returns "content://media/downloads/42"
        coEvery { fileAccessManager.isFileAccessible("content://media/downloads/42") } returns true

        val result = useCase("https://youtu.be/abc123")

        assertEquals(42L, result?.recordId)
    }
}
