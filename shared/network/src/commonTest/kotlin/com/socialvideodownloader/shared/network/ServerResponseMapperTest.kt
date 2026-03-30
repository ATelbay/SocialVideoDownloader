package com.socialvideodownloader.shared.network

import com.socialvideodownloader.shared.network.dto.ServerExtractResponse
import com.socialvideodownloader.shared.network.dto.ServerFormatDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ServerResponseMapperTest {
    private val mapper = ServerResponseMapper()

    private val sourceUrl = "https://youtube.com/watch?v=test"

    @Test
    fun mapToMetadata_mapsBasicFields_correctly() {
        val response =
            ServerExtractResponse(
                title = "My Video",
                thumbnail = "https://example.com/thumb.jpg",
                duration = 300.0,
                uploader = "MyChannel",
                formats = emptyList(),
            )

        val metadata = mapper.mapToMetadata(response, sourceUrl)

        assertEquals("My Video", metadata.title)
        assertEquals("https://example.com/thumb.jpg", metadata.thumbnailUrl)
        assertEquals(300, metadata.durationSeconds)
        assertEquals(sourceUrl, metadata.sourceUrl)
    }

    @Test
    fun mapToMetadata_handlesNullOptionalFields() {
        val response =
            ServerExtractResponse(
                title = "No Extras",
                thumbnail = null,
                duration = null,
                uploader = null,
                formats = emptyList(),
            )

        val metadata = mapper.mapToMetadata(response, sourceUrl)

        assertNull(metadata.thumbnailUrl)
        assertEquals(0, metadata.durationSeconds)
    }

    @Test
    fun mapToMetadata_roundsFloatDuration() {
        val response =
            ServerExtractResponse(
                title = "Test",
                thumbnail = null,
                duration = 123.7,
                uploader = null,
                formats = emptyList(),
            )

        val metadata = mapper.mapToMetadata(response, sourceUrl)

        assertEquals(123, metadata.durationSeconds)
    }

    @Test
    fun mapFormat_detectsVideoWithAudio_correctly() {
        val response =
            ServerExtractResponse(
                title = "Test",
                thumbnail = null,
                duration = null,
                uploader = null,
                formats =
                    listOf(
                        ServerFormatDto(
                            formatId = "22",
                            ext = "mp4",
                            resolution = "1280x720",
                            filesize = 20_000_000L,
                            url = "https://example.com/v.mp4",
                            vcodec = "avc1",
                            acodec = "mp4a",
                        ),
                    ),
            )

        val metadata = mapper.mapToMetadata(response, sourceUrl)

        val format = metadata.formats[0]
        assertFalse(format.isVideoOnly)
        assertFalse(format.isAudioOnly)
        assertEquals("22", format.formatId)
        assertEquals("mp4", format.ext)
        assertEquals(20_000_000L, format.fileSizeBytes)
    }

    @Test
    fun mapFormat_detectsVideoOnly_whenAcodecIsNone() {
        val response =
            ServerExtractResponse(
                title = "Test",
                thumbnail = null,
                duration = null,
                uploader = null,
                formats =
                    listOf(
                        ServerFormatDto(
                            formatId = "137",
                            ext = "mp4",
                            resolution = "1920x1080",
                            filesize = null,
                            url = "https://example.com/v.mp4",
                            vcodec = "avc1",
                            acodec = "none",
                        ),
                    ),
            )

        val metadata = mapper.mapToMetadata(response, sourceUrl)

        val format = metadata.formats[0]
        assertTrue(format.isVideoOnly)
        assertFalse(format.isAudioOnly)
    }

    @Test
    fun mapFormat_detectsAudioOnly_byExtension() {
        val response =
            ServerExtractResponse(
                title = "Test",
                thumbnail = null,
                duration = null,
                uploader = null,
                formats =
                    listOf(
                        ServerFormatDto(
                            formatId = "140",
                            ext = "m4a",
                            resolution = null,
                            filesize = 3_000_000L,
                            url = "https://example.com/a.m4a",
                            vcodec = null,
                            acodec = "mp4a",
                        ),
                    ),
            )

        val metadata = mapper.mapToMetadata(response, sourceUrl)

        val format = metadata.formats[0]
        assertTrue(format.isAudioOnly)
        assertFalse(format.isVideoOnly)
    }

    @Test
    fun mapFormat_parsesResolutionCorrectly_fromWidthxHeight() {
        val response =
            ServerExtractResponse(
                title = "Test",
                thumbnail = null,
                duration = null,
                uploader = null,
                formats =
                    listOf(
                        ServerFormatDto(
                            formatId = "22",
                            ext = "mp4",
                            resolution = "1920x1080",
                            filesize = null,
                            url = "https://example.com/v.mp4",
                            vcodec = "avc1",
                            acodec = "mp4a",
                        ),
                    ),
            )

        val metadata = mapper.mapToMetadata(response, sourceUrl)

        val format = metadata.formats[0]
        assertEquals(1080, format.resolution)
        assertEquals("1080p mp4", format.label)
    }

    @Test
    fun mapFormat_parsesResolutionCorrectly_fromPFormat() {
        val response =
            ServerExtractResponse(
                title = "Test",
                thumbnail = null,
                duration = null,
                uploader = null,
                formats =
                    listOf(
                        ServerFormatDto(
                            formatId = "22",
                            ext = "webm",
                            resolution = "720p",
                            filesize = null,
                            url = "https://example.com/v.webm",
                            vcodec = "vp9",
                            acodec = "opus",
                        ),
                    ),
            )

        val metadata = mapper.mapToMetadata(response, sourceUrl)

        val format = metadata.formats[0]
        assertEquals(720, format.resolution)
        assertEquals("720p webm", format.label)
    }

    @Test
    fun mapFormat_buildsAudioLabel_forAudioOnlyFormats() {
        val response =
            ServerExtractResponse(
                title = "Test",
                thumbnail = null,
                duration = null,
                uploader = null,
                formats =
                    listOf(
                        ServerFormatDto(
                            formatId = "140",
                            ext = "m4a",
                            resolution = null,
                            filesize = null,
                            url = "https://example.com/a.m4a",
                            vcodec = "none",
                            acodec = "mp4a",
                        ),
                    ),
            )

        val metadata = mapper.mapToMetadata(response, sourceUrl)

        val format = metadata.formats[0]
        assertEquals("m4a audio", format.label)
    }

    @Test
    fun mapFormat_setsDirectDownloadUrl_fromDtoUrl() {
        val url = "https://cdn.example.com/video.mp4?token=abc"
        val response =
            ServerExtractResponse(
                title = "Test",
                thumbnail = null,
                duration = null,
                uploader = null,
                formats =
                    listOf(
                        ServerFormatDto(
                            formatId = "22",
                            ext = "mp4",
                            resolution = null,
                            filesize = null,
                            url = url,
                            vcodec = "avc1",
                            acodec = "mp4a",
                        ),
                    ),
            )

        val metadata = mapper.mapToMetadata(response, sourceUrl)

        assertEquals(url, metadata.formats[0].directDownloadUrl)
    }

    @Test
    fun mapToMetadata_handlesEmptyFormats() {
        val response =
            ServerExtractResponse(
                title = "Test",
                thumbnail = null,
                duration = null,
                uploader = null,
                formats = emptyList(),
            )

        val metadata = mapper.mapToMetadata(response, sourceUrl)

        assertTrue(metadata.formats.isEmpty())
    }
}
