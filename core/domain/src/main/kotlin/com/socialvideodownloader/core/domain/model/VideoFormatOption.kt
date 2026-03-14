package com.socialvideodownloader.core.domain.model

data class VideoFormatOption(
    val formatId: String,
    val label: String,
    val resolution: Int? = null,
    val ext: String,
    val fileSizeBytes: Long? = null,
    val isAudioOnly: Boolean,
    val isVideoOnly: Boolean,
)
