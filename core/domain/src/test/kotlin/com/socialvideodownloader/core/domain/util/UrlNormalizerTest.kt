package com.socialvideodownloader.core.domain.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class UrlNormalizerTest {

    @Test
    fun `strips YouTube tracking params`() {
        val url = "https://www.youtube.com/watch?v=abc123&si=trackme&feature=share"
        assertEquals("https://youtube.com/watch?v=abc123", UrlNormalizer.normalize(url))
    }

    @Test
    fun `strips universal utm params`() {
        val url = "https://example.com/video?id=1&utm_source=fb&utm_campaign=spring"
        assertEquals("https://example.com/video?id=1", UrlNormalizer.normalize(url))
    }

    @Test
    fun `strips Instagram tracking params`() {
        val url = "https://www.instagram.com/reel/ABC/?igsh=abc&igshid=xyz"
        assertEquals("https://www.instagram.com/reel/ABC", UrlNormalizer.normalize(url))
    }

    @Test
    fun `strips TikTok tracking params`() {
        val url = "https://www.tiktok.com/@user/video/123?_t=abc&_r=1&is_from_webapp=v1"
        assertEquals("https://www.tiktok.com/@user/video/123", UrlNormalizer.normalize(url))
    }

    @Test
    fun `strips Twitter tracking params`() {
        val url = "https://twitter.com/user/status/123?s=20&t=abc"
        assertEquals("https://twitter.com/user/status/123", UrlNormalizer.normalize(url))
    }

    @Test
    fun `normalizes youtu_be short link to youtube_com`() {
        val url = "https://youtu.be/dQw4w9WgXcQ?si=trackme"
        assertEquals("https://youtube.com/watch?v=dQw4w9WgXcQ", UrlNormalizer.normalize(url))
    }

    @Test
    fun `youtu_be preserves timestamp param and strips tracking param`() {
        val url = "https://youtu.be/abc123?t=120&si=trackingvalue"
        assertEquals("https://youtube.com/watch?v=abc123&t=120", UrlNormalizer.normalize(url))
    }

    @Test
    fun `normalizes m_youtube_com to youtube_com`() {
        val url = "https://m.youtube.com/watch?v=abc123"
        assertEquals("https://youtube.com/watch?v=abc123", UrlNormalizer.normalize(url))
    }

    @Test
    fun `removes trailing slash`() {
        val url = "https://youtube.com/watch?v=abc123"
        val urlWithSlash = "https://youtube.com/watch/?v=abc123"
        // path trailing slash removal
        assertEquals(
            UrlNormalizer.normalize(url),
            UrlNormalizer.normalize(urlWithSlash),
        )
    }

    @Test
    fun `preserves non-tracking params`() {
        val url = "https://youtube.com/watch?v=abc123&t=42"
        assertEquals("https://youtube.com/watch?v=abc123&t=42", UrlNormalizer.normalize(url))
    }

    @Test
    fun `strips fbclid and gclid`() {
        val url = "https://example.com/video?id=1&fbclid=xyz&gclid=abc"
        assertEquals("https://example.com/video?id=1", UrlNormalizer.normalize(url))
    }

    @Test
    fun `handles blank url gracefully`() {
        assertEquals("", UrlNormalizer.normalize(""))
        assertEquals("", UrlNormalizer.normalize("  "))
    }

    @Test
    fun `handles url with no query params`() {
        val url = "https://youtube.com/watch"
        assertEquals("https://youtube.com/watch", UrlNormalizer.normalize(url))
    }

    @Test
    fun `idempotent normalization`() {
        val url = "https://youtube.com/watch?v=abc123&si=track"
        val once = UrlNormalizer.normalize(url)
        val twice = UrlNormalizer.normalize(once)
        assertEquals(once, twice)
    }
}
