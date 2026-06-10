package com.socialvideodownloader.shared.network

/**
 * Best-effort SSRF guard for server-dictated proxied requests. Only permits
 * http(s) URLs whose host is not a local/private/link-local literal. Full DNS
 * resolution is unavailable in commonMain, so this is literal-based plus an
 * obvious-hostname blocklist; the server performs the authoritative check.
 */
internal fun isProxyUrlAllowed(url: String): Boolean {
    val scheme = url.substringBefore("://", missingDelimiterValue = "").lowercase()
    if (scheme != "http" && scheme != "https") return false
    val authority =
        url.substringAfter("://")
            .substringBefore("/")
            .substringBefore("?")
            .substringBefore("#")
    // Strip optional userinfo (user:pass@host).
    val hostPort = authority.substringAfterLast("@")
    val host =
        if (hostPort.startsWith("[")) {
            // IPv6 literal: [::1]:8080
            hostPort.substringAfter("[").substringBefore("]")
        } else {
            hostPort.substringBefore(":")
        }.lowercase()
    if (host.isEmpty()) return false
    if (host == "localhost" || host.endsWith(".localhost") || host.endsWith(".local")) return false
    return !isPrivateOrLoopbackIpLiteral(host)
}

private fun isPrivateOrLoopbackIpLiteral(host: String): Boolean {
    // Normalise the many ways an IPv4 address can be written (dotted-quad,
    // IPv4-mapped IPv6 like ::ffff:127.0.0.1, single decimal/hex integer like
    // 2130706433 / 0x7f000001) to octets, then range-check those. Without this,
    // those alternate encodings slipped past the dotted-quad-only check.
    parseIpv4Octets(host)?.let { return isPrivateOrLoopbackV4(it) }

    // IPv6 loopback / unique-local (fc00::/7) / link-local (fe80::/10).
    // Note: exotic hex-embedded forms (e.g. ::ffff:7f00:1) are not normalised
    // here; the server-side guard remains authoritative.
    if (host.contains(":")) {
        if (host == "::1" || host == "::") return true
        val firstGroup = host.substringBefore(":")
        if (firstGroup.startsWith("fc") || firstGroup.startsWith("fd")) return true
        if (firstGroup.startsWith("fe8") || firstGroup.startsWith("fe9") ||
            firstGroup.startsWith("fea") || firstGroup.startsWith("feb")
        ) {
            return true
        }
        return false
    }
    return false
}

/** Range check on already-normalised IPv4 octets. */
private fun isPrivateOrLoopbackV4(nums: List<Int>): Boolean {
    val a = nums[0]
    val b = nums[1]
    return when {
        a == 10 -> true // 10.0.0.0/8
        a == 127 -> true // loopback
        a == 0 -> true // 0.0.0.0/8
        a == 169 && b == 254 -> true // link-local incl. 169.254.169.254 metadata
        a == 172 && b in 16..31 -> true // 172.16.0.0/12
        a == 192 && b == 168 -> true // 192.168.0.0/16
        a == 100 && b in 64..127 -> true // CGNAT 100.64.0.0/10
        else -> false
    }
}

/**
 * Best-effort normalisation of an IPv4 literal to four octets, or null if [host]
 * is not an IPv4 literal in a recognised encoding.
 */
private fun parseIpv4Octets(host: String): List<Int>? {
    // IPv4-mapped IPv6 (::ffff:127.0.0.1) — take the trailing dotted-quad.
    val candidate =
        if (host.contains(":")) {
            val tail = host.substringAfterLast(":")
            if (tail.contains(".")) tail else return null
        } else {
            host
        }

    if (candidate.contains(".")) {
        val parts = candidate.split(".")
        if (parts.size != 4) return null
        val nums = parts.map { it.toIntOrNull() ?: return null }
        if (nums.any { it !in 0..255 }) return null
        return nums
    }

    // Single 32-bit integer: hex (0x7f000001) or decimal (2130706433).
    val value: Long =
        when {
            candidate.startsWith("0x") || candidate.startsWith("0X") ->
                candidate.substring(2).toLongOrNull(16) ?: return null
            candidate.isNotEmpty() && candidate.all { it.isDigit() } ->
                candidate.toLongOrNull() ?: return null
            else -> return null
        }
    if (value < 0 || value > 0xFFFFFFFFL) return null
    return listOf(
        ((value shr 24) and 0xFF).toInt(),
        ((value shr 16) and 0xFF).toInt(),
        ((value shr 8) and 0xFF).toInt(),
        (value and 0xFF).toInt(),
    )
}
