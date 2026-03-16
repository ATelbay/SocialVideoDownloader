package com.socialvideodownloader.core.ui.tokens

import androidx.compose.ui.graphics.Color

object PlatformColors {
    val YouTube = Color(0xFFFF0000)
    val Instagram = Color(0xFFC13584)
    val TikTok = Color(0xFF69C9D0)
    val Twitter = Color(0xFF1DA1F2)
    val Vimeo = Color(0xFF1AB7EA)
    val Facebook = Color(0xFF1877F2)
    val Default = Color(0xFF79747E) // onSurfaceVariant for unknown platforms

    val TextOnPlatform = Color.White

    fun forPlatform(platformName: String?): Color {
        return when {
            platformName == null -> Default
            platformName.contains("youtube", ignoreCase = true) -> YouTube
            platformName.contains("instagram", ignoreCase = true) -> Instagram
            platformName.contains("tiktok", ignoreCase = true) || platformName.contains("tik tok", ignoreCase = true) -> TikTok
            platformName.contains("twitter", ignoreCase = true) || platformName.contains("x.com", ignoreCase = true) -> Twitter
            platformName.contains("vimeo", ignoreCase = true) -> Vimeo
            platformName.contains("facebook", ignoreCase = true) || platformName.contains("fb", ignoreCase = true) -> Facebook
            else -> Default
        }
    }

    fun textColor(platformName: String?): Color = when {
        platformName != null && platformName.contains("tiktok", ignoreCase = true) -> Color.Black
        else -> Color.White
    }

    fun nameFromUrl(url: String): String? = when {
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
