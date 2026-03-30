package com.socialvideodownloader.core.domain.usecase

import com.socialvideodownloader.core.domain.fake.FakeDownloadRepository
import com.socialvideodownloader.core.domain.fake.FakeFileAccessManager
import com.socialvideodownloader.core.domain.model.DownloadRecord
import com.socialvideodownloader.core.domain.model.DownloadStatus
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FindExistingDownloadUseCaseTest {
    private lateinit var repository: FakeDownloadRepository
    private lateinit var fileAccessManager: FakeFileAccessManager
    private lateinit var useCase: FindExistingDownloadUseCase

    private val completedRecord =
        DownloadRecord(
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

    @BeforeTest
    fun setup() {
        repository = FakeDownloadRepository()
        fileAccessManager = FakeFileAccessManager()
        useCase = FindExistingDownloadUseCase(repository, fileAccessManager)
    }

    @Test
    fun returnsNullWhenNoCompletedDownloadsMatch() =
        runTest {
            repository.setRecords(emptyList())

            val result = useCase("https://youtube.com/watch?v=abc123")

            assertNull(result)
        }

    @Test
    fun returnsNullWhenUrlDoesNotMatchAnyCompletedRecord() =
        runTest {
            repository.setRecords(listOf(completedRecord))

            val result = useCase("https://youtube.com/watch?v=different")

            assertNull(result)
        }

    @Test
    fun returnsNullWhenMatchedRecordHasNoContentUri() =
        runTest {
            repository.setRecords(listOf(completedRecord))
            // No entry in contentUriMap means resolveContentUri returns null

            val result = useCase("https://youtube.com/watch?v=abc123")

            assertNull(result)
        }

    @Test
    fun returnsNullWhenFileIsNotAccessible() =
        runTest {
            repository.setRecords(listOf(completedRecord))
            fileAccessManager.contentUriMap[42L] = "content://media/downloads/42"
            // accessibleUris is empty, so isFileAccessible returns false

            val result = useCase("https://youtube.com/watch?v=abc123")

            assertNull(result)
        }

    @Test
    fun returnsExistingDownloadWhenUrlMatchesAndFileIsAccessible() =
        runTest {
            repository.setRecords(listOf(completedRecord))
            fileAccessManager.contentUriMap[42L] = "content://media/downloads/42"
            fileAccessManager.accessibleUris.add("content://media/downloads/42")

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
    fun matchesUrlDespiteTrackingParamsInStoredRecord() =
        runTest {
            repository.setRecords(listOf(completedRecord))
            fileAccessManager.contentUriMap[42L] = "content://media/downloads/42"
            fileAccessManager.accessibleUris.add("content://media/downloads/42")

            // Input URL without tracking param should match the stored URL that had si= param
            val result = useCase("https://youtube.com/watch?v=abc123")

            assertEquals(42L, result?.recordId)
        }

    @Test
    fun matchesYoutuBeShortUrlAgainstStoredFullUrl() =
        runTest {
            repository.setRecords(listOf(completedRecord))
            fileAccessManager.contentUriMap[42L] = "content://media/downloads/42"
            fileAccessManager.accessibleUris.add("content://media/downloads/42")

            val result = useCase("https://youtu.be/abc123")

            assertEquals(42L, result?.recordId)
        }
}
