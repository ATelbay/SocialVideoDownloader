package com.socialvideodownloader.core.domain.usecase

import com.socialvideodownloader.core.domain.model.VideoMetadata
import com.socialvideodownloader.core.domain.repository.VideoExtractorRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ExtractVideoInfoUseCaseTest {
    private lateinit var repository: VideoExtractorRepository
    private lateinit var useCase: ExtractVideoInfoUseCase

    @BeforeEach
    fun setup() {
        repository = mockk()
        useCase = ExtractVideoInfoUseCase(repository)
    }

    @Test
    fun `invoke with valid url returns success with metadata`() =
        runTest {
            val expected =
                VideoMetadata(
                    sourceUrl = "https://youtube.com/watch?v=test",
                    title = "Test Video",
                    thumbnailUrl = "https://img.youtube.com/test.jpg",
                    durationSeconds = 120,
                    author = "Test Author",
                    formats = emptyList(),
                )
            coEvery { repository.extractInfo(any()) } returns expected

            val result = useCase("https://youtube.com/watch?v=test")

            assertTrue(result.isSuccess)
            assertEquals(expected, result.getOrNull())
        }

    @Test
    fun `invoke with network error returns failure`() =
        runTest {
            coEvery { repository.extractInfo(any()) } throws RuntimeException("Network error")

            val result = useCase("https://youtube.com/watch?v=test")

            assertTrue(result.isFailure)
            assertEquals("Network error", result.exceptionOrNull()?.message)
        }

    @Test
    fun `invoke with non-http url returns failure`() =
        runTest {
            val result = useCase("file:///sdcard/video.mp4")

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IllegalArgumentException)
            assertEquals("Only HTTP and HTTPS URLs are supported", result.exceptionOrNull()?.message)
        }

    @Test
    fun `invoke with plain text returns failure`() =
        runTest {
            val result = useCase("not a url at all")

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        }
}
