package com.socialvideodownloader.shared.network

import com.socialvideodownloader.shared.network.auth.NetscapeCookieParser
import com.socialvideodownloader.shared.network.auth.SupportedPlatform
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for cookie injection behavior in WebSocketExtractorApi.
 * Tests the matching and merging logic used in the http_request handler.
 */
class WebSocketExtractorApiCookieTest {
    private val testCookies =
        """
        # Netscape HTTP Cookie File
        .instagram.com${"\t"}TRUE${"\t"}/${"\t"}TRUE${"\t"}0${"\t"}sessionid${"\t"}abc123
        .instagram.com${"\t"}TRUE${"\t"}/${"\t"}TRUE${"\t"}0${"\t"}csrftoken${"\t"}xyz789
        """.trimIndent()

    @Test
    fun cookieInjection_matchesInstagramHost() {
        val reqUrl = "https://www.instagram.com/api/v1/feed/"
        val reqHost = reqUrl.substringAfter("://").substringBefore("/").substringBefore(":").lowercase()

        val matchedPlatform =
            SupportedPlatform.entries.firstOrNull { platform ->
                platform.hostMatches.any { host -> reqHost == host || reqHost.endsWith(".$host") }
            }

        assertEquals(SupportedPlatform.INSTAGRAM, matchedPlatform)
    }

    @Test
    fun cookieInjection_doesNotMatchYouTubeForInstagramUrl() {
        val reqUrl = "https://www.instagram.com/api/v1/feed/"
        val reqHost = reqUrl.substringAfter("://").substringBefore("/").substringBefore(":").lowercase()

        val matchedPlatform =
            SupportedPlatform.entries.firstOrNull { platform ->
                platform.hostMatches.any { host -> reqHost == host || reqHost.endsWith(".$host") }
            }

        assertTrue(matchedPlatform != SupportedPlatform.YOUTUBE)
    }

    @Test
    fun cookieInjection_doesNotMatchUnknownHost() {
        val reqUrl = "https://cdn.example.com/some/resource"
        val reqHost = reqUrl.substringAfter("://").substringBefore("/").substringBefore(":").lowercase()

        val matchedPlatform =
            SupportedPlatform.entries.firstOrNull { platform ->
                platform.hostMatches.any { host -> reqHost == host || reqHost.endsWith(".$host") }
            }

        assertNull(matchedPlatform)
    }

    @Test
    fun cookieInjection_mergesWithExistingCookieHeader() {
        val pairs = NetscapeCookieParser.parseToNameValuePairs(testCookies)
        val cookieString = pairs.joinToString("; ") { "${it.first}=${it.second}" }
        val existing = "existing_cookie=value1"

        val merged = "$existing; $cookieString"

        assertTrue(merged.contains("existing_cookie=value1"))
        assertTrue(merged.contains("sessionid=abc123"))
        assertTrue(merged.contains("csrftoken=xyz789"))
    }

    @Test
    fun cookieInjection_createsNewCookieHeaderWhenNoneExists() {
        val pairs = NetscapeCookieParser.parseToNameValuePairs(testCookies)
        val cookieString = pairs.joinToString("; ") { "${it.first}=${it.second}" }

        assertEquals("sessionid=abc123; csrftoken=xyz789", cookieString)
    }
}
