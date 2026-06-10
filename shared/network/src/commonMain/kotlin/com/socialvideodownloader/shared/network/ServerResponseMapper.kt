package com.socialvideodownloader.shared.network

import com.socialvideodownloader.core.domain.model.VideoFormatOption
import com.socialvideodownloader.core.domain.model.VideoMetadata
import com.socialvideodownloader.shared.network.dto.ServerExtractResponse
import com.socialvideodownloader.shared.network.dto.ServerFormatDto

class ServerResponseMapper {
    private val audioOnlyExtensions = setOf("m4a", "mp3", "opus", "ogg", "aac", "flac", "wav")

    /** yt-dlp emits storyboard/preview pseudo-formats that are not downloadable media. */
    private val excludedExtensions = setOf("mhtml", "storyboard")

    fun mapToMetadata(
        response: ServerExtractResponse,
        sourceUrl: String,
    ): VideoMetadata {
        // Filter junk pseudo-formats and sort best-first so the ViewModel's
        // "first non-audio format = best" heuristic holds on the server/iOS path
        // (parity with Android's VideoInfoMapper — the server response order is
        // yt-dlp's default ascending order and cannot be relied on).
        val mapped =
            response.formats
                .asSequence()
                .filter { it.ext !in excludedExtensions }
                .map { mapFormat(it) }
                .toList()
        val videoFormats = mapped.filter { !it.isAudioOnly }.sortedByDescending { it.resolution }
        val audioFormats = mapped.filter { it.isAudioOnly }.sortedByDescending { it.fileSizeBytes }

        return VideoMetadata(
            sourceUrl = sourceUrl,
            title = response.title,
            thumbnailUrl = response.thumbnail,
            durationSeconds = response.duration?.toInt() ?: 0,
            author = response.uploader,
            formats = videoFormats + audioFormats,
        )
    }

    private fun mapFormat(dto: ServerFormatDto): VideoFormatOption {
        val height = parseHeight(dto.resolution)
        val hasVideoCodec = dto.vcodec != null && dto.vcodec != "none"
        val hasAudioCodec = dto.acodec != null && dto.acodec != "none"
        // Codec metadata is authoritative when present: audio-only webm/opus streams carry no
        // resolution but their ext ("webm") is not in audioOnlyExtensions, so the ext heuristic
        // would misclassify them as video.
        val isAudioOnly =
            if (dto.vcodec != null && dto.acodec != null) {
                hasAudioCodec && !hasVideoCodec
            } else {
                height == null && dto.ext in audioOnlyExtensions
            }
        val label = buildLabel(height, dto.ext, isAudioOnly)

        val isVideoOnly = hasVideoCodec && !hasAudioCodec

        return VideoFormatOption(
            formatId = dto.formatId,
            label = label,
            resolution = height,
            ext = dto.ext,
            fileSizeBytes = dto.filesize,
            isAudioOnly = isAudioOnly,
            isVideoOnly = isVideoOnly,
            directDownloadUrl = dto.url,
        )
    }

    private fun parseHeight(resolution: String?): Int? {
        if (resolution == null) return null
        // "1920x1080" -> 1080
        val parts = resolution.split("x")
        if (parts.size == 2) {
            return parts[1].toIntOrNull()
        }
        // "1080p", "144p60" -> 1080, 144
        return Regex("""(\d+)p""").find(resolution)?.groupValues?.get(1)?.toIntOrNull()
    }

    private fun buildLabel(
        height: Int?,
        ext: String,
        isAudioOnly: Boolean,
    ): String {
        return if (isAudioOnly) {
            "$ext audio"
        } else if (height != null) {
            "${height}p $ext"
        } else {
            ext
        }
    }
}
