package com.socialvideodownloader.shared.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ServerExtractRequest(
    val url: String,
    @SerialName("api_key") val apiKey: String? = null,
)

@Serializable
data class ServerExtractResponse(
    val title: String,
    val thumbnail: String? = null,
    val duration: Double? = null,
    val uploader: String? = null,
    val formats: List<ServerFormatDto>,
    @SerialName("direct_download_url") val directDownloadUrl: String? = null,
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
    @SerialName("format_note") val formatNote: String? = null,
)
