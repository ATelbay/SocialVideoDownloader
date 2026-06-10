package com.socialvideodownloader.core.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MuxCompatibleAudioTest {
    private fun audio(
        formatId: String,
        ext: String,
        sizeBytes: Long?,
        directUrl: String? = "https://cdn.example.com/$formatId",
    ) = VideoFormatOption(
        formatId = formatId,
        label = "$ext audio",
        ext = ext,
        fileSizeBytes = sizeBytes,
        isAudioOnly = true,
        isVideoOnly = false,
        directDownloadUrl = directUrl,
    )

    private fun video(
        formatId: String,
        ext: String,
    ) = VideoFormatOption(
        formatId = formatId,
        label = "1080p $ext",
        resolution = 1080,
        ext = ext,
        fileSizeBytes = 50_000_000L,
        isAudioOnly = false,
        isVideoOnly = true,
        directDownloadUrl = "https://cdn.example.com/$formatId",
    )

    @Test
    fun picksM4aForMp4Video_ignoringWebmAudio() {
        val formats =
            listOf(
                video("137", "mp4"),
                audio("251", "webm", 4_000_000L),
                audio("140", "m4a", 3_000_000L),
            )

        assertEquals("140", formats.bestMuxCompatibleAudio("mp4")?.formatId)
    }

    @Test
    fun picksWebmAudioForWebmVideo_ignoringM4a() {
        val formats =
            listOf(
                video("248", "webm"),
                audio("140", "m4a", 5_000_000L),
                audio("251", "webm", 4_000_000L),
            )

        assertEquals("251", formats.bestMuxCompatibleAudio("webm")?.formatId)
    }

    @Test
    fun picksLargestCompatibleAudio() {
        val formats =
            listOf(
                audio("139", "m4a", 1_000_000L),
                audio("140", "m4a", 3_000_000L),
            )

        assertEquals("140", formats.bestMuxCompatibleAudio("mp4")?.formatId)
    }

    @Test
    fun ignoresAudioWithoutDirectUrl() {
        val formats = listOf(audio("140", "m4a", 3_000_000L, directUrl = null))

        assertNull(formats.bestMuxCompatibleAudio("mp4"))
    }

    @Test
    fun ignoresNonAudioFormats() {
        val formats = listOf(video("137", "mp4"))

        assertNull(formats.bestMuxCompatibleAudio("mp4"))
    }

    @Test
    fun returnsNullForUnknownContainer() {
        val formats = listOf(audio("140", "m4a", 3_000_000L))

        assertNull(formats.bestMuxCompatibleAudio("flv"))
    }
}
