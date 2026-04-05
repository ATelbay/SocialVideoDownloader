package com.socialvideodownloader.shared.network.auth

enum class SupportedPlatform(
    val displayName: String,
    val loginUrl: String,
    val cookieDomains: List<String>,
    val hostMatches: List<String>,
    val successCookieName: String,
) {
    INSTAGRAM(
        displayName = "Instagram",
        loginUrl = "https://www.instagram.com/accounts/login/",
        cookieDomains = listOf(".instagram.com"),
        hostMatches = listOf("instagram.com", "www.instagram.com", "i.instagram.com"),
        successCookieName = "sessionid",
    ),
    YOUTUBE(
        displayName = "YouTube",
        loginUrl = "https://accounts.google.com/ServiceLogin?service=youtube",
        cookieDomains = listOf(".youtube.com", ".google.com"),
        hostMatches = listOf("youtube.com", "www.youtube.com", "m.youtube.com", "googlevideo.com"),
        successCookieName = "SID",
    ),
    TWITTER(
        displayName = "Twitter / X",
        loginUrl = "https://x.com/i/flow/login",
        cookieDomains = listOf(".x.com", ".twitter.com"),
        hostMatches = listOf("x.com", "twitter.com", "api.x.com", "api.twitter.com"),
        successCookieName = "auth_token",
    ),
    REDDIT(
        displayName = "Reddit",
        loginUrl = "https://www.reddit.com/login/",
        cookieDomains = listOf(".reddit.com"),
        hostMatches = listOf("reddit.com", "www.reddit.com", "old.reddit.com"),
        successCookieName = "reddit_session",
    ),
    FACEBOOK(
        displayName = "Facebook",
        loginUrl = "https://www.facebook.com/login/",
        cookieDomains = listOf(".facebook.com"),
        hostMatches = listOf("facebook.com", "www.facebook.com", "m.facebook.com", "web.facebook.com"),
        successCookieName = "c_user",
    ),
}

/** Detect platform from a video URL by matching the host. */
fun detectPlatform(url: String): SupportedPlatform? {
    val host =
        try {
            // Simple host extraction — handle URLs with or without protocol
            val withProtocol = if (url.contains("://")) url else "https://$url"
            val afterProtocol = withProtocol.substringAfter("://")
            afterProtocol.substringBefore("/").substringBefore("?").substringBefore(":").lowercase()
        } catch (_: Exception) {
            return null
        }
    return SupportedPlatform.entries.firstOrNull { platform ->
        platform.hostMatches.any { hostMatch -> host == hostMatch || host.endsWith(".$hostMatch") }
    }
}

/** Detect platform from a yt-dlp error message by matching prefixes like [Instagram], [youtube]. */
fun detectPlatformFromError(errorMessage: String): SupportedPlatform? {
    val lowered = errorMessage.lowercase()
    return SupportedPlatform.entries.firstOrNull { platform ->
        val tag = "[${platform.displayName.lowercase()}"
        lowered.contains(tag) ||
            // Handle alternate names
            when (platform) {
                SupportedPlatform.TWITTER -> lowered.contains("[twitter") || lowered.contains("[x]")
                SupportedPlatform.YOUTUBE -> lowered.contains("[youtube")
                else -> false
            }
    }
}
