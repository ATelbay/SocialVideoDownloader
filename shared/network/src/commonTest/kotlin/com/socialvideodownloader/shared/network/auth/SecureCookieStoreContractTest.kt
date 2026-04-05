package com.socialvideodownloader.shared.network.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Contract tests for SecureCookieStore behavior using an in-memory fake.
 * Verifies the expected behavior that platform actuals must implement.
 */
class SecureCookieStoreContractTest {
    // In-memory fake that mirrors SecureCookieStore contract
    private class FakeSecureCookieStore {
        private val store = mutableMapOf<SupportedPlatform, String>()

        fun getCookies(platform: SupportedPlatform): String? = store[platform]

        fun setCookies(
            platform: SupportedPlatform,
            cookies: String,
        ) {
            store[platform] = cookies
        }

        fun clearCookies(platform: SupportedPlatform) {
            store.remove(platform)
        }

        fun isConnected(platform: SupportedPlatform): Boolean = store.containsKey(platform)

        fun connectedPlatforms(): List<SupportedPlatform> = store.keys.toList()
    }

    private val store = FakeSecureCookieStore()
    private val testCookies =
        "# Netscape HTTP Cookie File\n.instagram.com\tTRUE\t/\tTRUE\t0\tsessionid\tabc123"

    @Test
    fun setCookies_thenGetCookies_roundTrip() {
        store.setCookies(SupportedPlatform.INSTAGRAM, testCookies)
        assertEquals(testCookies, store.getCookies(SupportedPlatform.INSTAGRAM))
    }

    @Test
    fun getCookies_returnsNull_whenNotSet() {
        assertNull(store.getCookies(SupportedPlatform.YOUTUBE))
    }

    @Test
    fun clearCookies_removesCorrectEntryOnly() {
        store.setCookies(SupportedPlatform.INSTAGRAM, testCookies)
        store.setCookies(SupportedPlatform.YOUTUBE, "youtube cookies")

        store.clearCookies(SupportedPlatform.INSTAGRAM)

        assertNull(store.getCookies(SupportedPlatform.INSTAGRAM))
        assertNotNull(store.getCookies(SupportedPlatform.YOUTUBE))
    }

    @Test
    fun isConnected_reflectsState() {
        assertFalse(store.isConnected(SupportedPlatform.INSTAGRAM))

        store.setCookies(SupportedPlatform.INSTAGRAM, testCookies)
        assertTrue(store.isConnected(SupportedPlatform.INSTAGRAM))

        store.clearCookies(SupportedPlatform.INSTAGRAM)
        assertFalse(store.isConnected(SupportedPlatform.INSTAGRAM))
    }

    @Test
    fun connectedPlatforms_returnsCorrectList() {
        assertTrue(store.connectedPlatforms().isEmpty())

        store.setCookies(SupportedPlatform.INSTAGRAM, testCookies)
        store.setCookies(SupportedPlatform.TWITTER, "twitter cookies")

        val connected = store.connectedPlatforms()
        assertEquals(2, connected.size)
        assertTrue(connected.contains(SupportedPlatform.INSTAGRAM))
        assertTrue(connected.contains(SupportedPlatform.TWITTER))
    }

    @Test
    fun setCookies_overwritesExisting() {
        store.setCookies(SupportedPlatform.INSTAGRAM, "old cookies")
        store.setCookies(SupportedPlatform.INSTAGRAM, "new cookies")

        assertEquals("new cookies", store.getCookies(SupportedPlatform.INSTAGRAM))
    }
}
