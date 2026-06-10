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
    fun mapFormat_detectsAudioOnly_forWebmOpus_byCodecs() {
        // YouTube opus audio streams have ext "webm", which the ext heuristic alone
        // would misclassify as video — codec metadata must win.
        val response =
            ServerExtractResponse(
                title = "Test",
                thumbnail = null,
                duration = null,
                uploader = null,
                formats =
                    listOf(
                        ServerFormatDto(
                            formatId = "251",
                            ext = "webm",
                            resolution = null,
                            filesize = 2_500_000L,
                            url = "https://example.com/a.webm",
                            vcodec = "none",
                            acodec = "opus",
                        ),
                    ),
            )

        val metadata = mapper.mapToMetadata(response, sourceUrl)

        val format = metadata.formats[0]
        assertTrue(format.isAudioOnly)
        assertFalse(format.isVideoOnly)
        assertEquals("webm audio", format.label)
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
    fun mapToMetadata_excludesStoryboardAndMhtmlFormats() {
        val response =
            ServerExtractResponse(
                title = "Test",
                thumbnail = null,
                duration = null,
                uploader = null,
                formats =
                    listOf(
                        ServerFormatDto(
                            formatId = "sb0",
                            ext = "mhtml",
                            resolution = "48x27",
                            filesize = null,
                            url = "https://example.com/sb.mhtml",
                            vcodec = "none",
                            acodec = "none",
                        ),
                        ServerFormatDto(
                            formatId = "sb1",
                            ext = "storyboard",
                            resolution = null,
                            filesize = null,
                            url = "https://example.com/sb1",
                            vcodec = "none",
                            acodec = "none",
                        ),
                        ServerFormatDto(
                            formatId = "22",
                            ext = "mp4",
                            resolution = "1280x720",
                            filesize = 10_000_000L,
                            url = "https://example.com/v.mp4",
                            vcodec = "avc1",
                            acodec = "mp4a",
                        ),
                    ),
            )

        val metadata = mapper.mapToMetadata(response, sourceUrl)

        assertEquals(1, metadata.formats.size)
        assertEquals("22", metadata.formats[0].formatId)
    }

    @Test
    fun mapToMetadata_sortsVideoFormatsBestFirst_audioLast() {
        val response =
            ServerExtractResponse(
                title = "Test",
                thumbnail = null,
                duration = null,
                uploader = null,
                // Deliberately worst-first (yt-dlp default order) with audio interleaved.
                formats =
                    listOf(
                        ServerFormatDto(
                            formatId = "audio",
                            ext = "m4a",
                            resolution = null,
                            filesize = 3_000_000L,
                            url = "https://example.com/a.m4a",
                            vcodec = "none",
                            acodec = "mp4a",
                        ),
                        ServerFormatDto(
                            formatId = "360",
                            ext = "mp4",
                            resolution = "640x360",
                            filesize = null,
                            url = "https://example.com/360.mp4",
                            vcodec = "avc1",
                            acodec = "mp4a",
                        ),
                        ServerFormatDto(
                            formatId = "1080",
                            ext = "mp4",
                            resolution = "1920x1080",
                            filesize = null,
                            url = "https://example.com/1080.mp4",
                            vcodec = "avc1",
                            acodec = "mp4a",
                        ),
                    ),
            )

        val metadata = mapper.mapToMetadata(response, sourceUrl)

        assertEquals(listOf("1080", "360", "audio"), metadata.formats.map { it.formatId })
        // First non-audio format is the highest-resolution one (the VM's "best" pick).
        assertFalse(metadata.formats[0].isAudioOnly)
        assertEquals(1080, metadata.formats[0].resolution)
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
