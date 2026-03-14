package com.socialvideodownloader.core.domain.usecase

import com.socialvideodownloader.core.domain.model.DownloadRequest
import com.socialvideodownloader.core.domain.repository.VideoExtractorRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DownloadVideoUseCaseTest {
    private lateinit var repository: VideoExtractorRepository
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

    @BeforeEach
    fun setup() {
        repository = mockk()
        useCase = DownloadVideoUseCase(repository)
    }

    @Test
    fun `delegates request to repository`() =
        runTest {
            val requestSlot = slot<DownloadRequest>()
            coEvery { repository.download(capture(requestSlot), any()) } returns "/output/file.mp4"

            useCase(baseRequest) { _, _, _ -> }

            assertEquals("248", requestSlot.captured.formatId)
            assertEquals("req-1", requestSlot.captured.id)
        }

    @Test
    fun `returns output path from repository`() =
        runTest {
            coEvery { repository.download(any(), any()) } returns "/downloads/video.mp4"

            val result = useCase(baseRequest) { _, _, _ -> }

            assertEquals("/downloads/video.mp4", result)
        }

    @Test
    fun `callback is forwarded to repository`() =
        runTest {
            coEvery { repository.download(any(), any()) } returns "/output/file.mp4"

            val callback: (Float, Long, String) -> Unit = mockk(relaxed = true)
            useCase(baseRequest, callback)

            coVerify { repository.download(any(), callback) }
        }
}
