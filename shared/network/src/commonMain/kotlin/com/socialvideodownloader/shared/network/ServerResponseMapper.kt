package com.socialvideodownloader.shared.network

import com.socialvideodownloader.core.domain.model.VideoFormatOption
import com.socialvideodownloader.core.domain.model.VideoMetadata
import com.socialvideodownloader.shared.network.dto.ServerExtractResponse
import com.socialvideodownloader.shared.network.dto.ServerFormatDto

class ServerResponseMapper {

    private val audioOnlyExtensions = setOf("m4a", "mp3", "opus", "ogg", "aac", "flac", "wav")

    fun mapToMetadata(response: ServerExtractResponse, sourceUrl: String): VideoMetadata {
        return VideoMetadata(
            sourceUrl = sourceUrl,
            title = response.title,
            thumbnailUrl = response.thumbnail,
            durationSeconds = response.duration?.toInt() ?: 0,
            author = response.uploader,
            formats = response.formats.map { mapFormat(it) },
        )
    }

    private fun mapFormat(dto: ServerFormatDto): VideoFormatOption {
        val height = parseHeight(dto.resolution)
        val isAudioOnly = height == null && dto.ext in audioOnlyExtensions
        val label = buildLabel(height, dto.ext, isAudioOnly)

        val isVideoOnly = dto.vcodec != null && dto.vcodec != "none" &&
            (dto.acodec == null || dto.acodec == "none")

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

    private fun buildLabel(height: Int?, ext: String, isAudioOnly: Boolean): String {
        return if (isAudioOnly) {
            "$ext audio"
        } else if (height != null) {
            "${height}p $ext"
        } else {
            ext
        }
    }
}
