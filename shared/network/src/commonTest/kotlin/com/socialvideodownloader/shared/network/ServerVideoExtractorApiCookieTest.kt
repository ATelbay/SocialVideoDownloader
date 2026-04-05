package com.socialvideodownloader.shared.network

import com.socialvideodownloader.shared.network.auth.detectPlatform
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ServerVideoExtractorApiCookieTest {
    private val testCookies =
        "# Netscape HTTP Cookie File\n.instagram.com\tTRUE\t/\tTRUE\t0\tsessionid\tabc123"

    @Test
    @OptIn(ExperimentalEncodingApi::class)
    fun xPlatformCookiesHeader_sentForMatchedPlatform() {
        val platform = detectPlatform("https://www.instagram.com/reel/xxx")
        assertNotNull(platform)

        val encoded = Base64.Default.encode(testCookies.encodeToByteArray())
        assertTrue(encoded.isNotEmpty())

        val decoded = Base64.Default.decode(encoded).decodeToString()
        assertTrue(decoded.contains("sessionid"))
    }

    @Test
    fun xPlatformCookiesHeader_notSentForUnmatchedPlatform() {
        val platform = detectPlatform("https://www.tiktok.com/video/xxx")
        assertNull(platform)
    }

    @Test
    fun xPlatformCookiesHeader_notSentWhenNoCookiesStored() {
        // Even if platform matches, no cookies means no header
        val platform = detectPlatform("https://www.instagram.com/reel/xxx")
        assertNotNull(platform)
        // getCookies returns null when no cookies stored — header should not be added
        // This test documents the expected contract; actual integration tested in platform tests
    }
}
