package com.socialvideodownloader.core.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ServerExtractRequest(
    val url: String,
)

@Serializable
data class ServerExtractResponse(
    val title: String,
    val thumbnail: String? = null,
    val duration: Double? = null,
    val formats: List<ServerFormatDto>,
)

@Serializable
data class ServerFormatDto(
    @SerialName("format_id") val formatId: String,
    val ext: String,
    val resolution: String? = null,
    val filesize: Long? = null,
    val url: String,
    val vcodec: String? = null,
    val acodec: String? = null,
)
