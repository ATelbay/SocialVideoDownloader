package com.socialvideodownloader.core.data.remote

import com.yausername.youtubedl_android.mapper.VideoFormat
import com.yausername.youtubedl_android.mapper.VideoInfo
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class VideoInfoMapperTest {
    private lateinit var mapper: VideoInfoMapper

    @BeforeEach
    fun setup() {
        mapper = VideoInfoMapper()
    }

    @Test
    fun `maps VideoInfo fields to VideoMetadata`() {
        val videoInfo =
            createVideoInfo(
                title = "Test Title",
                thumbnail = "https://thumb.jpg",
                duration = 300,
                uploader = "Author",
                formats = emptyList(),
            )

        val result = mapper.mapToMetadata(videoInfo, "https://youtube.com/v")

        assertEquals("https://youtube.com/v", result.sourceUrl)
        assertEquals("Test Title", result.title)
        assertEquals("https://thumb.jpg", result.thumbnailUrl)
        assertEquals(300, result.durationSeconds)
        assertEquals("Author", result.author)
    }

    @Test
    fun `filters out formats with null formatId`() {
        val formats =
            listOf(
                createVideoFormat(formatId = "248", height = 1080),
                createVideoFormat(formatId = null, height = 720),
                createVideoFormat(formatId = "136", height = 720),
            )
        val videoInfo = createVideoInfo(formats = formats)

        val result = mapper.mapToMetadata(videoInfo, "url")

        assertEquals(2, result.formats.size)
    }

    @Test
    fun `detects audio-only format when vcodec is none`() {
        val formats =
            listOf(
                createVideoFormat(formatId = "251", vcodec = "none", acodec = "opus", ext = "webm", height = 0),
            )
        val videoInfo = createVideoInfo(formats = formats)

        val result = mapper.mapToMetadata(videoInfo, "url")

        assertTrue(result.formats.first().isAudioOnly)
        assertFalse(result.formats.first().isVideoOnly)
    }

    @Test
    fun `detects video-only format when acodec is none`() {
        val formats =
            listOf(
                createVideoFormat(formatId = "248", acodec = "none", vcodec = "vp9", height = 1080),
            )
        val videoInfo = createVideoInfo(formats = formats)

        val result = mapper.mapToMetadata(videoInfo, "url")

        assertTrue(result.formats.first().isVideoOnly)
        assertFalse(result.formats.first().isAudioOnly)
    }

    @Test
    fun `generates label from height for video formats`() {
        val formats =
            listOf(
                createVideoFormat(formatId = "248", height = 1080, vcodec = null),
            )
        val videoInfo = createVideoInfo(formats = formats)

        val result = mapper.mapToMetadata(videoInfo, "url")

        assertEquals("1080p", result.formats.first().label)
    }

    @Test
    fun `sorts video formats by resolution descending`() {
        val formats =
            listOf(
                createVideoFormat(formatId = "1", height = 480),
                createVideoFormat(formatId = "2", height = 1080),
                createVideoFormat(formatId = "3", height = 720),
            )
        val videoInfo = createVideoInfo(formats = formats)

        val result = mapper.mapToMetadata(videoInfo, "url")

        assertEquals(1080, result.formats[0].resolution)
        assertEquals(720, result.formats[1].resolution)
        assertEquals(480, result.formats[2].resolution)
    }

    @Test
    fun `audio formats come after video formats`() {
        val formats =
            listOf(
                createVideoFormat(formatId = "251", vcodec = "none", acodec = "opus", height = 0, ext = "webm"),
                createVideoFormat(formatId = "248", height = 1080),
            )
        val videoInfo = createVideoInfo(formats = formats)

        val result = mapper.mapToMetadata(videoInfo, "url")

        assertFalse(result.formats[0].isAudioOnly)
        assertTrue(result.formats[1].isAudioOnly)
    }

    private fun createVideoInfo(
        title: String = "Test",
        thumbnail: String? = null,
        duration: Int = 0,
        uploader: String? = null,
        formats: List<VideoFormat> = emptyList(),
    ): VideoInfo =
        mockk<VideoInfo> {
            every { this@mockk.title } returns title
            every { this@mockk.thumbnail } returns thumbnail
            every { this@mockk.duration } returns duration
            every { this@mockk.uploader } returns uploader
            every { this@mockk.formats } returns ArrayList(formats)
        }

    private fun createVideoFormat(
        formatId: String? = "248",
        height: Int = 0,
        ext: String? = "mp4",
        fileSize: Long = 0L,
        fileSizeApproximate: Long = 0L,
        vcodec: String? = "vp9",
        acodec: String? = "mp4a",
        abr: Int = 0,
    ): VideoFormat =
        mockk<VideoFormat> {
            every { this@mockk.formatId } returns formatId
            every { this@mockk.height } returns height
            every { this@mockk.ext } returns ext
            every { this@mockk.fileSize } returns fileSize
            every { this@mockk.fileSizeApproximate } returns fileSizeApproximate
            every { this@mockk.vcodec } returns vcodec
            every { this@mockk.acodec } returns acodec
            every { this@mockk.abr } returns abr
        }
}
