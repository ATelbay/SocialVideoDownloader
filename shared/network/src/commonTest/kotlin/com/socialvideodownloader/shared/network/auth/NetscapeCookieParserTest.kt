package com.socialvideodownloader.shared.network.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NetscapeCookieParserTest {
    // ---- parseToNameValuePairs ----

    @Test
    fun parse_singleValidLine_returnsOnePair() {
        val input = ".instagram.com\tTRUE\t/\tTRUE\t0\tsessionid\tabc123"
        val result = NetscapeCookieParser.parseToNameValuePairs(input)
        assertEquals(1, result.size)
        assertEquals("sessionid" to "abc123", result[0])
    }

    @Test
    fun parse_multipleValidLines_returnsAllPairs() {
        val input =
            """
            .instagram.com${"\t"}TRUE${"\t"}/${"\t"}TRUE${"\t"}0${"\t"}sessionid${"\t"}abc123
            .instagram.com${"\t"}TRUE${"\t"}/${"\t"}FALSE${"\t"}0${"\t"}csrftoken${"\t"}xyz789
            """.trimIndent()
        val result = NetscapeCookieParser.parseToNameValuePairs(input)
        assertEquals(2, result.size)
        assertEquals("sessionid" to "abc123", result[0])
        assertEquals("csrftoken" to "xyz789", result[1])
    }

    @Test
    fun parse_commentLinesAreSkipped() {
        val input =
            """
            # Netscape HTTP Cookie File
            # This is another comment
            .instagram.com${"\t"}TRUE${"\t"}/${"\t"}TRUE${"\t"}0${"\t"}sessionid${"\t"}abc123
            """.trimIndent()
        val result = NetscapeCookieParser.parseToNameValuePairs(input)
        assertEquals(1, result.size)
        assertEquals("sessionid" to "abc123", result[0])
    }

    @Test
    fun parse_blankLinesAreSkipped() {
        val input = "\n\n.instagram.com\tTRUE\t/\tTRUE\t0\tsessionid\tabc123\n\n"
        val result = NetscapeCookieParser.parseToNameValuePairs(input)
        assertEquals(1, result.size)
    }

    @Test
    fun parse_malformedLineWithFewerThan7Fields_isSkipped() {
        val input = ".instagram.com\tTRUE\t/\tTRUE\t0\tsessionid" // only 6 fields
        val result = NetscapeCookieParser.parseToNameValuePairs(input)
        assertTrue(result.isEmpty())
    }

    @Test
    fun parse_emptyString_returnsEmptyList() {
        val result = NetscapeCookieParser.parseToNameValuePairs("")
        assertTrue(result.isEmpty())
    }

    @Test
    fun parse_mixedValidInvalidAndCommentLines() {
        val input =
            """
            # comment
            .instagram.com${"\t"}TRUE${"\t"}/${"\t"}TRUE${"\t"}0${"\t"}sessionid${"\t"}abc123
            bad_line_no_tabs

            .instagram.com${"\t"}TRUE${"\t"}/${"\t"}FALSE${"\t"}0${"\t"}csrftoken${"\t"}xyz789
            """.trimIndent()
        val result = NetscapeCookieParser.parseToNameValuePairs(input)
        assertEquals(2, result.size)
        assertEquals("sessionid" to "abc123", result[0])
        assertEquals("csrftoken" to "xyz789", result[1])
    }

    // ---- formatToNetscape ----

    @Test
    fun format_singleEntry_producesValidNetscapeString() {
        val cookie =
            CookieEntry(
                domain = ".instagram.com",
                includeSubdomains = true,
                path = "/",
                secure = true,
                expiry = 0L,
                name = "sessionid",
                value = "abc123",
            )
        val result = NetscapeCookieParser.formatToNetscape(listOf(cookie))
        assertTrue(result.contains("# Netscape HTTP Cookie File"))
        assertTrue(result.contains(".instagram.com"))
        assertTrue(result.contains("TRUE"))
        assertTrue(result.contains("sessionid"))
        assertTrue(result.contains("abc123"))
    }

    @Test
    fun format_multipleEntries_allAppearInOutput() {
        val cookies =
            listOf(
                CookieEntry(".instagram.com", true, "/", true, 0L, "sessionid", "abc123"),
                CookieEntry(".instagram.com", true, "/", false, 0L, "csrftoken", "xyz789"),
            )
        val result = NetscapeCookieParser.formatToNetscape(cookies)
        assertTrue(result.contains("sessionid"))
        assertTrue(result.contains("csrftoken"))
        assertTrue(result.contains("abc123"))
        assertTrue(result.contains("xyz789"))
    }

    @Test
    fun roundTrip_formatThenParse_returnsSameNameValuePairs() {
        val cookies =
            listOf(
                CookieEntry(".instagram.com", true, "/", true, 0L, "sessionid", "abc123"),
                CookieEntry(".instagram.com", true, "/", false, 1234567890L, "csrftoken", "xyz789"),
            )
        val formatted = NetscapeCookieParser.formatToNetscape(cookies)
        val parsed = NetscapeCookieParser.parseToNameValuePairs(formatted)

        assertEquals(2, parsed.size)
        assertEquals("sessionid" to "abc123", parsed[0])
        assertEquals("csrftoken" to "xyz789", parsed[1])
    }

    @Test
    fun format_includeSubdomainsFalse_writesFALSE() {
        val cookie = CookieEntry(".example.com", false, "/", false, 0L, "token", "value")
        val result = NetscapeCookieParser.formatToNetscape(listOf(cookie))
        // Both includeSubdomains and secure are FALSE
        val lines = result.lines().filter { it.isNotBlank() && !it.startsWith("#") }
        assertEquals(1, lines.size)
        val fields = lines[0].split("\t")
        assertEquals("FALSE", fields[1]) // includeSubdomains
        assertEquals("FALSE", fields[3]) // secure
    }
}
