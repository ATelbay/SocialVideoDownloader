package com.socialvideodownloader.core.domain.util

import java.net.URI

object UrlNormalizer {

    private val youtubeTrackingParams = setOf("si", "feature", "list", "index")
    private val instagramTrackingParams = setOf("igsh", "igshid")
    private val tiktokTrackingParams = setOf("_t", "_r", "is_from_webapp", "sender_device")
    private val twitterTrackingParams = setOf("s", "t", "ref_src", "ref_url")
    private val universalTrackingParams = setOf(
        "utm_source", "utm_medium", "utm_campaign", "utm_content", "utm_term",
        "fbclid", "gclid",
    )

    fun normalize(url: String): String {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return trimmed

        return try {
            var uri = URI(trimmed)

            // Normalize youtu.be short links — preserve non-tracking params (e.g. t=120)
            if (uri.host?.equals("youtu.be", ignoreCase = true) == true) {
                val videoId = uri.path.trimStart('/')
                val keptParams = uri.query?.split("&")?.filter { param ->
                    val key = param.substringBefore("=")
                    key !in youtubeTrackingParams && !key.startsWith("utm_")
                }?.takeIf { it.isNotEmpty() }
                val queryStr = if (keptParams != null) "&${keptParams.joinToString("&")}" else ""
                uri = URI("https://www.youtube.com/watch?v=$videoId$queryStr")
            }

            // Normalize host: lowercase, remove m. prefix for youtube
            val host = uri.host?.lowercase() ?: return trimmed
            val normalizedHost = when {
                host == "m.youtube.com" -> "youtube.com"
                host == "www.youtube.com" -> "youtube.com"
                else -> host
            }

            // Determine which params to strip based on host
            val paramsToStrip = buildSet {
                addAll(universalTrackingParams)
                when {
                    normalizedHost.contains("youtube.com") -> addAll(youtubeTrackingParams)
                    normalizedHost.contains("instagram.com") -> {
                        addAll(instagramTrackingParams)
                        // Also strip utm_* dynamically handled by universal
                    }
                    normalizedHost.contains("tiktok.com") -> addAll(tiktokTrackingParams)
                    normalizedHost.contains("twitter.com") || normalizedHost.contains("x.com") ->
                        addAll(twitterTrackingParams)
                }
            }

            // Strip tracking params and utm_* prefix params
            val query = uri.query
            val filteredQuery = if (query.isNullOrBlank()) {
                null
            } else {
                val kept = query.split("&").filter { param ->
                    val key = param.substringBefore("=")
                    key !in paramsToStrip && !key.startsWith("utm_")
                }
                if (kept.isEmpty()) null else kept.joinToString("&")
            }

            // Remove trailing slash from path
            val path = uri.path?.trimEnd('/').let { if (it.isNullOrBlank()) "/" else it }

            val scheme = uri.scheme?.lowercase() ?: "https"
            val port = if (uri.port == -1) "" else ":${uri.port}"
            val queryPart = if (filteredQuery != null) "?$filteredQuery" else ""

            "$scheme://$normalizedHost$port$path$queryPart"
        } catch (_: Exception) {
            trimmed
        }
    }
}
