package com.socialvideodownloader.core.data.remote

import com.socialvideodownloader.core.data.remote.dto.ServerExtractResponse
import com.socialvideodownloader.core.data.remote.dto.ServerFormatDto
import com.socialvideodownloader.core.domain.model.VideoFormatOption
import com.socialvideodownloader.core.domain.model.VideoMetadata
import javax.inject.Inject

class ServerResponseMapper @Inject constructor() {

    private val audioOnlyExtensions = setOf("m4a", "mp3", "opus", "ogg", "aac", "flac", "wav")

    fun mapToMetadata(response: ServerExtractResponse, sourceUrl: String): VideoMetadata {
        return VideoMetadata(
            sourceUrl = sourceUrl,
            title = response.title,
            thumbnailUrl = response.thumbnail,
            durationSeconds = response.duration?.toInt() ?: 0,
            formats = response.formats.map { mapFormat(it) },
        )
    }

    private fun mapFormat(dto: ServerFormatDto): VideoFormatOption {
        val height = parseHeight(dto.resolution)
        val isAudioOnly = height == null && dto.ext in audioOnlyExtensions
        val label = buildLabel(height, dto.ext, isAudioOnly)

        return VideoFormatOption(
            formatId = dto.formatId,
            label = label,
            resolution = height,
            ext = dto.ext,
            fileSizeBytes = dto.filesize,
            isAudioOnly = isAudioOnly,
            isVideoOnly = false,
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
        // "1080p" -> 1080
        return resolution.replace("p", "").toIntOrNull()
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
