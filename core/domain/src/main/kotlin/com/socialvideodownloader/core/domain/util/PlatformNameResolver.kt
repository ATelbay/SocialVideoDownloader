package com.socialvideodownloader.core.domain.util

object PlatformNameResolver {
    fun nameFromUrl(url: String): String? =
        when {
            url.contains("youtube.com", ignoreCase = true) || url.contains("youtu.be", ignoreCase = true) -> "YouTube"
            url.contains("instagram.com", ignoreCase = true) -> "Instagram"
            url.contains("tiktok.com", ignoreCase = true) -> "TikTok"
            url.contains("twitter.com", ignoreCase = true) || url.contains("x.com", ignoreCase = true) -> "Twitter"
            url.contains("vimeo.com", ignoreCase = true) -> "Vimeo"
            url.contains("facebook.com", ignoreCase = true) || url.contains("fb.watch", ignoreCase = true) -> "Facebook"
            url.contains("reddit.com", ignoreCase = true) -> "Reddit"
            url.contains("dailymotion.com", ignoreCase = true) -> "Dailymotion"
            else -> null
        }
}
