package com.socialvideodownloader.core.data.remote

import com.socialvideodownloader.core.domain.model.VideoFormatOption
import com.socialvideodownloader.core.domain.model.VideoMetadata
import com.yausername.youtubedl_android.mapper.VideoFormat
import com.yausername.youtubedl_android.mapper.VideoInfo
import javax.inject.Inject

class VideoInfoMapper @Inject constructor() {

    fun mapToMetadata(videoInfo: VideoInfo, url: String): VideoMetadata {
        val excludedExtensions = setOf("mhtml", "storyboard")
        val formats = videoInfo.formats
            ?.filter { !it.formatId.isNullOrBlank() }
            ?.filter { it.ext !in excludedExtensions }
            ?.map { mapToFormatOption(it) }
            ?.let { allFormats ->
                val videoFormats = allFormats.filter { !it.isAudioOnly }
                    .sortedByDescending { it.resolution }
                val audioFormats = allFormats.filter { it.isAudioOnly }
                    .sortedByDescending { it.fileSizeBytes }
                videoFormats + audioFormats
            } ?: emptyList()

        return VideoMetadata(
            sourceUrl = url,
            title = videoInfo.title ?: "Unknown",
            thumbnailUrl = videoInfo.thumbnail,
            durationSeconds = videoInfo.duration,
            author = videoInfo.uploader,
            formats = formats,
        )
    }

    private fun buildVideoLabel(format: VideoFormat): String {
        val resolution = "${format.height}p"
        val codec = format.vcodec
            ?.substringBefore(".")  // "av01.0.08M.08" → "av01"
            ?.takeIf { it != "none" }
        return listOfNotNull(resolution, codec).joinToString(" ")
    }

    private fun buildAudioLabel(format: VideoFormat): String {
        val codec = format.acodec?.takeIf { it != "none" }
        val bitrateKbps = format.abr.takeIf { it > 0 }?.let { "${it.toInt()}k" }
        val ext = format.ext
        return listOfNotNull(codec ?: ext, bitrateKbps).joinToString(" ").ifBlank { "audio" }
    }

    private fun mapToFormatOption(format: VideoFormat): VideoFormatOption {
        val isAudioOnly = format.vcodec?.equals("none") == true
        val isVideoOnly = format.acodec?.equals("none") == true
        val label = when {
            isAudioOnly -> buildAudioLabel(format)
            format.height > 0 -> buildVideoLabel(format)
            else -> format.ext ?: "unknown"
        }
        val fileSize = format.fileSize.takeIf { it > 0 }
            ?: format.fileSizeApproximate.takeIf { it > 0 }
        return VideoFormatOption(
            formatId = format.formatId!!,
            label = label,
            resolution = if (!isAudioOnly && format.height > 0) format.height else null,
            ext = format.ext ?: "mp4",
            fileSizeBytes = fileSize,
            isAudioOnly = isAudioOnly,
            isVideoOnly = isVideoOnly,
        )
    }
}
