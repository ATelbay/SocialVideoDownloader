package com.socialvideodownloader.core.domain.sync

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class CloudDocumentIdTest {
    @Test
    fun matchesAndroidLegacySha256HexVector() {
        // SHA-256("https://example.com/v1" + "1000"), lowercase hex — identical to the previous
        // Android java.security.MessageDigest output, so existing Android backups keep their ids.
        assertEquals(
            "87aa0bde0233ee973dd1f38fe51cfa5c685a1efee3f9eaf2324523e515ec00df",
            cloudDocumentId("https://example.com/v1", 1000L),
        )
    }

    @Test
    fun isDeterministic() {
        assertEquals(
            cloudDocumentId("https://x.test/a", 42L),
            cloudDocumentId("https://x.test/a", 42L),
        )
    }

    @Test
    fun createdAtIsPartOfTheId() {
        assertNotEquals(
            cloudDocumentId("https://x.test/a", 1L),
            cloudDocumentId("https://x.test/a", 2L),
        )
    }

    @Test
    fun outputIs64HexChars() {
        val id = cloudDocumentId("https://x.test/a", 1L)
        assertEquals(64, id.length)
        assertEquals(true, id.all { it in "0123456789abcdef" })
    }
}
