package com.socialvideodownloader.core.data.remote

import com.socialvideodownloader.core.domain.model.VideoFormatOption
import com.socialvideodownloader.core.domain.model.VideoMetadata
import com.yausername.youtubedl_android.mapper.VideoFormat
import com.yausername.youtubedl_android.mapper.VideoInfo
import javax.inject.Inject

class VideoInfoMapper @Inject constructor() {

    fun mapToMetadata(videoInfo: VideoInfo, url: String): VideoMetadata {
        val formats = videoInfo.formats
            ?.filter { !it.formatId.isNullOrBlank() }
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

    private fun mapToFormatOption(format: VideoFormat): VideoFormatOption {
        val isAudioOnly = format.vcodec?.equals("none") == true
        val isVideoOnly = format.acodec?.equals("none") == true
        val label = when {
            isAudioOnly -> format.ext ?: "audio"
            format.height > 0 -> "${format.height}p"
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
