package com.socialvideodownloader.shared.feature.download

import com.socialvideodownloader.core.domain.model.VideoFormatOption
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Verifies that startDownload attaches a mux-compatible audio stream URL when the selected
 * format is a video-only DASH stream downloaded via direct URL (server extraction path), so
 * the platform downloader can merge audio on-device instead of saving a silent video.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SharedDownloadViewModelMuxAudioTest {
    private val url = "https://www.youtube.com/shorts/abc"

    private fun format(
        formatId: String,
        ext: String,
        isVideoOnly: Boolean = false,
        isAudioOnly: Boolean = false,
        directUrl: String? = "https://cdn.example.com/$formatId",
        sizeBytes: Long? = 1_000_000L,
    ) = VideoFormatOption(
        formatId = formatId,
        label = formatId,
        resolution = if (isAudioOnly) null else 1080,
        ext = ext,
        fileSizeBytes = sizeBytes,
        isAudioOnly = isAudioOnly,
        isVideoOnly = isVideoOnly,
        directDownloadUrl = directUrl,
    )

    private fun kotlinx.coroutines.test.TestScope.startedRequest(
        formats: List<VideoFormatOption>,
    ): com.socialvideodownloader.core.domain.model.DownloadRequest {
        val manager = ControllablePlatformDownloadManager()
        val vm =
            makeVm(
                this,
                FakeVideoExtractorRepository.alwaysSucceeds(videoMetadata(sourceUrl = url, formats = formats)),
                downloadManager = manager,
            )
        vm.onIntent(DownloadIntent.UrlChanged(url))
        vm.onIntent(DownloadIntent.ExtractClicked)
        advanceUntilIdle()
        vm.onIntent(DownloadIntent.DownloadClicked)
        advanceUntilIdle()
        vm.cleanup()
        assertEquals(1, manager.startedRequests.size)
        return manager.startedRequests.first()
    }

    @Test
    fun videoOnlyDirectFormat_attachesCompatibleAudioUrl() =
        runTest {
            val request =
                startedRequest(
                    listOf(
                        format("137", ext = "mp4", isVideoOnly = true),
                        format("251", ext = "webm", isAudioOnly = true, sizeBytes = 4_000_000L),
                        format("140", ext = "m4a", isAudioOnly = true, sizeBytes = 3_000_000L),
                    ),
                )

            assertEquals("https://cdn.example.com/137", request.directDownloadUrl)
            // mp4 video pairs with m4a audio, not the larger (incompatible) webm stream.
            assertEquals("https://cdn.example.com/140", request.audioDirectUrl)
        }

    @Test
    fun progressiveFormat_doesNotAttachAudioUrl() =
        runTest {
            val request =
                startedRequest(
                    listOf(
                        format("22", ext = "mp4"),
                        format("140", ext = "m4a", isAudioOnly = true),
                    ),
                )

            assertNull(request.audioDirectUrl)
        }

    @Test
    fun videoOnlyWithoutDirectUrl_doesNotAttachAudioUrl() =
        runTest {
            val request =
                startedRequest(
                    listOf(
                        format("137", ext = "mp4", isVideoOnly = true, directUrl = null),
                        format("140", ext = "m4a", isAudioOnly = true),
                    ),
                )

            assertNull(request.audioDirectUrl)
        }

    @Test
    fun videoOnlyWithoutCompatibleAudio_attachesNoAudioUrl() =
        runTest {
            val request =
                startedRequest(
                    listOf(
                        format("248", ext = "webm", isVideoOnly = true),
                        format("140", ext = "m4a", isAudioOnly = true),
                    ),
                )

            assertEquals("https://cdn.example.com/248", request.directDownloadUrl)
            assertNull(request.audioDirectUrl)
        }
}
