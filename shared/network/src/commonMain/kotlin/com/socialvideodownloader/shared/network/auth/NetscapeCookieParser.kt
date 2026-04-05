package com.socialvideodownloader.shared.network.auth

/**
 * Represents a single cookie in Netscape cookie file format.
 */
data class CookieEntry(
    val domain: String,
    val includeSubdomains: Boolean,
    val path: String,
    val secure: Boolean,
    val expiry: Long,
    val name: String,
    val value: String,
)

/**
 * Utilities for parsing and formatting Netscape cookie file format,
 * compatible with yt-dlp's --cookies format.
 */
object NetscapeCookieParser {
    private const val HEADER = "# Netscape HTTP Cookie File"

    /**
     * Parse a Netscape cookie string into name=value pairs for HTTP Cookie header injection.
     * Skips comment lines (starting with #) and blank lines.
     */
    fun parseToNameValuePairs(netscapeCookieString: String): List<Pair<String, String>> =
        netscapeCookieString
            .lineSequence()
            .filter { line -> line.isNotBlank() && !line.trimStart().startsWith("#") }
            .mapNotNull { line ->
                val fields = line.split("\t")
                if (fields.size >= 7) {
                    fields[5] to fields[6]
                } else {
                    null
                }
            }
            .toList()

    /**
     * Format a list of CookieEntry objects into a Netscape cookie file string.
     */
    fun formatToNetscape(cookies: List<CookieEntry>): String = buildString {
        appendLine(HEADER)
        cookies.forEach { cookie ->
            append(cookie.domain)
            append("\t")
            append(if (cookie.includeSubdomains) "TRUE" else "FALSE")
            append("\t")
            append(cookie.path)
            append("\t")
            append(if (cookie.secure) "TRUE" else "FALSE")
            append("\t")
            append(cookie.expiry)
            append("\t")
            append(cookie.name)
            append("\t")
            appendLine(cookie.value)
        }
    }
}
