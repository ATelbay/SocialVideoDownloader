package com.socialvideodownloader.core.domain.usecase

import com.socialvideodownloader.core.domain.fake.FakeVideoExtractorRepository
import com.socialvideodownloader.core.domain.model.VideoMetadata
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExtractVideoInfoUseCaseTest {
    private lateinit var repository: FakeVideoExtractorRepository
    private lateinit var useCase: ExtractVideoInfoUseCase

    @BeforeTest
    fun setup() {
        repository = FakeVideoExtractorRepository()
        useCase = ExtractVideoInfoUseCase(repository)
    }

    @Test
    fun invokeWithValidUrlReturnsSuccessWithMetadata() =
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
            repository.extractInfoResult = Result.success(expected)

            val result = useCase("https://youtube.com/watch?v=test")

            assertTrue(result.isSuccess)
            assertEquals(expected, result.getOrNull())
        }

    @Test
    fun invokeWithNetworkErrorReturnsFailure() =
        runTest {
            repository.extractInfoResult = Result.failure(RuntimeException("Network error"))

            val result = useCase("https://youtube.com/watch?v=test")

            assertTrue(result.isFailure)
            assertEquals("Network error", result.exceptionOrNull()?.message)
        }

    @Test
    fun invokeWithNonHttpUrlReturnsFailure() =
        runTest {
            val result = useCase("file:///sdcard/video.mp4")

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IllegalArgumentException)
            assertEquals("Only HTTP and HTTPS URLs are supported", result.exceptionOrNull()?.message)
        }

    @Test
    fun invokeWithPlainTextReturnsFailure() =
        runTest {
            val result = useCase("not a url at all")

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        }
}
