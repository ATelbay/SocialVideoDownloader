package com.socialvideodownloader.core.domain.util

object UrlNormalizer {
    private val youtubeTrackingParams = setOf("si", "feature", "list", "index")
    private val instagramTrackingParams = setOf("igsh", "igshid")
    private val tiktokTrackingParams = setOf("_t", "_r", "is_from_webapp", "sender_device")
    private val twitterTrackingParams = setOf("s", "t", "ref_src", "ref_url")
    private val universalTrackingParams =
        setOf(
            "utm_source",
            "utm_medium",
            "utm_campaign",
            "utm_content",
            "utm_term",
            "fbclid",
            "gclid",
        )

    fun normalize(url: String): String {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return trimmed

        return try {
            val parsed = parseUrl(trimmed) ?: return trimmed

            var scheme = parsed.scheme
            var host = parsed.host
            var port = parsed.port
            var path = parsed.path
            var query = parsed.query

            // Normalize youtu.be short links — preserve non-tracking params (e.g. t=120)
            if (host.equals("youtu.be", ignoreCase = true)) {
                val videoId = path.trimStart('/')
                val keptParams =
                    query?.split("&")?.filter { param ->
                        val key = param.substringBefore("=")
                        key !in youtubeTrackingParams && !key.startsWith("utm_")
                    }?.takeIf { it.isNotEmpty() }
                val queryStr = if (keptParams != null) "&${keptParams.joinToString("&")}" else ""
                val rebuilt =
                    parseUrl("https://www.youtube.com/watch?v=$videoId$queryStr")
                        ?: return trimmed
                scheme = rebuilt.scheme
                host = rebuilt.host
                port = rebuilt.port
                path = rebuilt.path
                query = rebuilt.query
            }

            // Normalize host: lowercase, remove m. prefix for youtube
            val normalizedHost =
                when (host.lowercase()) {
                    "m.youtube.com" -> "youtube.com"
                    "www.youtube.com" -> "youtube.com"
                    else -> host.lowercase()
                }

            // Determine which params to strip based on host
            val paramsToStrip =
                buildSet {
                    addAll(universalTrackingParams)
                    when {
                        normalizedHost.contains("youtube.com") -> addAll(youtubeTrackingParams)
                        normalizedHost.contains("instagram.com") -> {
                            addAll(instagramTrackingParams)
                        }
                        normalizedHost.contains("tiktok.com") -> addAll(tiktokTrackingParams)
                        normalizedHost.contains("twitter.com") || normalizedHost.contains("x.com") ->
                            addAll(twitterTrackingParams)
                    }
                }

            // Strip tracking params and utm_* prefix params
            val filteredQuery =
                if (query.isNullOrBlank()) {
                    null
                } else {
                    val kept =
                        query.split("&").filter { param ->
                            val key = param.substringBefore("=")
                            key !in paramsToStrip && !key.startsWith("utm_")
                        }
                    if (kept.isEmpty()) null else kept.joinToString("&")
                }

            // Remove trailing slash from path
            val normalizedPath = path.trimEnd('/').let { if (it.isBlank()) "/" else it }

            val normalizedScheme = scheme.lowercase()
            val portStr = if (port != null) ":$port" else ""
            val queryPart = if (filteredQuery != null) "?$filteredQuery" else ""

            "$normalizedScheme://$normalizedHost$portStr$normalizedPath$queryPart"
        } catch (_: Exception) {
            trimmed
        }
    }

    private data class ParsedUrl(
        val scheme: String,
        val host: String,
        val port: String?,
        val path: String,
        val query: String?,
    )

    private fun parseUrl(url: String): ParsedUrl? {
        // Extract scheme
        val schemeEnd = url.indexOf("://")
        if (schemeEnd < 0) return null
        val scheme = url.substring(0, schemeEnd)

        val afterScheme = url.substring(schemeEnd + 3)

        // Split authority from path+query
        val pathStart = afterScheme.indexOf('/')
        val authority: String
        val rest: String
        if (pathStart < 0) {
            authority = afterScheme
            rest = "/"
        } else {
            authority = afterScheme.substring(0, pathStart)
            rest = afterScheme.substring(pathStart)
        }

        // Extract host and port from authority
        val host: String
        val port: String?
        // Handle IPv6 (e.g., [::1]:8080) — unlikely but safe
        if (authority.startsWith("[")) {
            val bracketEnd = authority.indexOf(']')
            if (bracketEnd < 0) return null
            host = authority.substring(0, bracketEnd + 1)
            port =
                if (bracketEnd + 1 < authority.length && authority[bracketEnd + 1] == ':') {
                    authority.substring(bracketEnd + 2)
                } else {
                    null
                }
        } else {
            val colonIdx = authority.indexOf(':')
            if (colonIdx >= 0) {
                host = authority.substring(0, colonIdx)
                port = authority.substring(colonIdx + 1)
            } else {
                host = authority
                port = null
            }
        }

        if (host.isBlank()) return null

        // Split path and query
        val queryStart = rest.indexOf('?')
        val path: String
        val query: String?
        if (queryStart < 0) {
            path = rest
            query = null
        } else {
            path = rest.substring(0, queryStart)
            val rawQuery = rest.substring(queryStart + 1)
            // Strip fragment
            val fragmentIdx = rawQuery.indexOf('#')
            query = if (fragmentIdx >= 0) rawQuery.substring(0, fragmentIdx) else rawQuery
        }

        return ParsedUrl(scheme = scheme, host = host, port = port, path = path, query = query)
    }
}
