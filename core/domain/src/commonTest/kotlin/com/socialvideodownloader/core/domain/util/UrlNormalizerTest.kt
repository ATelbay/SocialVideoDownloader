package com.socialvideodownloader.core.domain.util

import kotlin.test.Test
import kotlin.test.assertEquals

class UrlNormalizerTest {
    @Test
    fun stripsYouTubeTrackingParams() {
        val url = "https://www.youtube.com/watch?v=abc123&si=trackme&feature=share"
        assertEquals("https://youtube.com/watch?v=abc123", UrlNormalizer.normalize(url))
    }

    @Test
    fun stripsUniversalUtmParams() {
        val url = "https://example.com/video?id=1&utm_source=fb&utm_campaign=spring"
        assertEquals("https://example.com/video?id=1", UrlNormalizer.normalize(url))
    }

    @Test
    fun stripsInstagramTrackingParams() {
        val url = "https://www.instagram.com/reel/ABC/?igsh=abc&igshid=xyz"
        assertEquals("https://www.instagram.com/reel/ABC", UrlNormalizer.normalize(url))
    }

    @Test
    fun stripsTikTokTrackingParams() {
        val url = "https://www.tiktok.com/@user/video/123?_t=abc&_r=1&is_from_webapp=v1"
        assertEquals("https://www.tiktok.com/@user/video/123", UrlNormalizer.normalize(url))
    }

    @Test
    fun stripsTwitterTrackingParams() {
        val url = "https://twitter.com/user/status/123?s=20&t=abc"
        assertEquals("https://twitter.com/user/status/123", UrlNormalizer.normalize(url))
    }

    @Test
    fun normalizesYoutuBeShortLinkToYoutubeCom() {
        val url = "https://youtu.be/dQw4w9WgXcQ?si=trackme"
        assertEquals("https://youtube.com/watch?v=dQw4w9WgXcQ", UrlNormalizer.normalize(url))
    }

    @Test
    fun youtuBePreservesTimestampParamAndStripsTrackingParam() {
        val url = "https://youtu.be/abc123?t=120&si=trackingvalue"
        assertEquals("https://youtube.com/watch?v=abc123&t=120", UrlNormalizer.normalize(url))
    }

    @Test
    fun normalizesMYoutubeComToYoutubeCom() {
        val url = "https://m.youtube.com/watch?v=abc123"
        assertEquals("https://youtube.com/watch?v=abc123", UrlNormalizer.normalize(url))
    }

    @Test
    fun removesTrailingSlash() {
        val url = "https://youtube.com/watch?v=abc123"
        val urlWithSlash = "https://youtube.com/watch/?v=abc123"
        assertEquals(
            UrlNormalizer.normalize(url),
            UrlNormalizer.normalize(urlWithSlash),
        )
    }

    @Test
    fun preservesNonTrackingParams() {
        val url = "https://youtube.com/watch?v=abc123&t=42"
        assertEquals("https://youtube.com/watch?v=abc123&t=42", UrlNormalizer.normalize(url))
    }

    @Test
    fun stripsFbclidAndGclid() {
        val url = "https://example.com/video?id=1&fbclid=xyz&gclid=abc"
        assertEquals("https://example.com/video?id=1", UrlNormalizer.normalize(url))
    }

    @Test
    fun handlesBlankUrlGracefully() {
        assertEquals("", UrlNormalizer.normalize(""))
        assertEquals("", UrlNormalizer.normalize("  "))
    }

    @Test
    fun handlesUrlWithNoQueryParams() {
        val url = "https://youtube.com/watch"
        assertEquals("https://youtube.com/watch", UrlNormalizer.normalize(url))
    }

    @Test
    fun idempotentNormalization() {
        val url = "https://youtube.com/watch?v=abc123&si=track"
        val once = UrlNormalizer.normalize(url)
        val twice = UrlNormalizer.normalize(once)
        assertEquals(once, twice)
    }
}
