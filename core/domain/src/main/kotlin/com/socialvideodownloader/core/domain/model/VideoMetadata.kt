package com.socialvideodownloader.core.domain.model

data class VideoMetadata(
    val sourceUrl: String,
    val title: String,
    val thumbnailUrl: String? = null,
    val durationSeconds: Int,
    val author: String? = null,
    val formats: List<VideoFormatOption>,
)
