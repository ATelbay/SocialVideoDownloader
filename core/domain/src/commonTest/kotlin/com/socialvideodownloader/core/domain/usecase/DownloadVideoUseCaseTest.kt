package com.socialvideodownloader.core.domain.usecase

import com.socialvideodownloader.core.domain.fake.FakeVideoExtractorRepository
import com.socialvideodownloader.core.domain.model.DownloadRequest
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class DownloadVideoUseCaseTest {
    private lateinit var repository: FakeVideoExtractorRepository
    private lateinit var useCase: DownloadVideoUseCase

    private val baseRequest =
        DownloadRequest(
            id = "req-1",
            sourceUrl = "https://youtube.com/watch?v=test",
            videoTitle = "Test Video",
            thumbnailUrl = null,
            formatId = "248",
            formatLabel = "1080p",
            isVideoOnly = false,
        )

    @BeforeTest
    fun setup() {
        repository = FakeVideoExtractorRepository()
        useCase = DownloadVideoUseCase(repository)
    }

    @Test
    fun delegatesRequestToRepository() =
        runTest {
            repository.downloadResult = "/output/file.mp4"

            useCase(baseRequest) { _, _, _ -> }

            val captured = repository.lastDownloadRequest
            assertNotNull(captured)
            assertEquals("248", captured.formatId)
            assertEquals("req-1", captured.id)
        }

    @Test
    fun returnsOutputPathFromRepository() =
        runTest {
            repository.downloadResult = "/downloads/video.mp4"

            val result = useCase(baseRequest) { _, _, _ -> }

            assertEquals("/downloads/video.mp4", result)
        }

    @Test
    fun callbackIsForwardedToRepository() =
        runTest {
            repository.downloadResult = "/output/file.mp4"

            val callback: (Float, Long, String) -> Unit = { _, _, _ -> }
            useCase(baseRequest, callback)

            assertEquals(callback, repository.lastDownloadCallback)
        }
}
