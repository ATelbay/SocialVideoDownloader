package com.socialvideodownloader.shared.network

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for the client-side SSRF guard that vets server-dictated proxied URLs.
 */
class ProxyUrlGuardTest {
    @Test
    fun allowsPublicHttpsUrls() {
        assertTrue(isProxyUrlAllowed("https://www.googlevideo.com/videoplayback?a=1"))
        assertTrue(isProxyUrlAllowed("https://scontent.cdninstagram.com/v/file.mp4"))
        assertTrue(isProxyUrlAllowed("http://example.com/path"))
        assertTrue(isProxyUrlAllowed("https://8.8.8.8/resource"))
    }

    @Test
    fun rejectsNonHttpSchemes() {
        assertFalse(isProxyUrlAllowed("ftp://example.com/file"))
        assertFalse(isProxyUrlAllowed("file:///etc/passwd"))
        assertFalse(isProxyUrlAllowed("ws://example.com/socket"))
        assertFalse(isProxyUrlAllowed("gopher://example.com"))
        assertFalse(isProxyUrlAllowed("example.com/no-scheme"))
    }

    @Test
    fun rejectsLocalHostnames() {
        assertFalse(isProxyUrlAllowed("http://localhost/admin"))
        assertFalse(isProxyUrlAllowed("http://localhost:8080/"))
        assertFalse(isProxyUrlAllowed("http://service.local/"))
        assertFalse(isProxyUrlAllowed("http://api.localhost/"))
    }

    @Test
    fun rejectsLoopbackAndPrivateIpv4() {
        assertFalse(isProxyUrlAllowed("http://127.0.0.1/"))
        assertFalse(isProxyUrlAllowed("http://127.0.0.1:5000/x"))
        assertFalse(isProxyUrlAllowed("http://10.0.0.5/"))
        assertFalse(isProxyUrlAllowed("http://192.168.1.1/router"))
        assertFalse(isProxyUrlAllowed("http://172.16.0.1/"))
        assertFalse(isProxyUrlAllowed("http://172.31.255.255/"))
        assertFalse(isProxyUrlAllowed("http://0.0.0.0/"))
        assertFalse(isProxyUrlAllowed("http://100.64.0.1/"))
    }

    @Test
    fun rejectsCloudMetadataEndpoint() {
        assertFalse(isProxyUrlAllowed("http://169.254.169.254/latest/meta-data/"))
    }

    @Test
    fun allowsPublicIpv4InPrivateLookingButPublicRange() {
        // 172.15.x and 172.32.x are public (outside 172.16.0.0/12).
        assertTrue(isProxyUrlAllowed("http://172.15.0.1/"))
        assertTrue(isProxyUrlAllowed("http://172.32.0.1/"))
        // 100.63 / 100.128 are outside the CGNAT 100.64.0.0/10 block.
        assertTrue(isProxyUrlAllowed("http://100.63.0.1/"))
        assertTrue(isProxyUrlAllowed("http://100.128.0.1/"))
    }

    @Test
    fun rejectsIpv6LoopbackAndPrivate() {
        assertFalse(isProxyUrlAllowed("http://[::1]/"))
        assertFalse(isProxyUrlAllowed("http://[::1]:8080/x"))
        assertFalse(isProxyUrlAllowed("http://[fc00::1]/"))
        assertFalse(isProxyUrlAllowed("http://[fd12:3456::1]/"))
        assertFalse(isProxyUrlAllowed("http://[fe80::1]/"))
    }

    @Test
    fun stripsUserInfoBeforeHostCheck() {
        // userinfo must not trick the parser into treating a private host as public.
        assertFalse(isProxyUrlAllowed("http://public.com@127.0.0.1/"))
        assertFalse(isProxyUrlAllowed("http://user:pass@10.0.0.1/"))
        assertTrue(isProxyUrlAllowed("http://user:pass@example.com/"))
    }

    @Test
    fun rejectsIpv4MappedIpv6Loopback() {
        assertFalse(isProxyUrlAllowed("http://[::ffff:127.0.0.1]/"))
        assertFalse(isProxyUrlAllowed("http://[::ffff:10.0.0.1]:8080/x"))
        assertFalse(isProxyUrlAllowed("http://[::ffff:169.254.169.254]/latest/"))
    }

    @Test
    fun rejectsIntegerEncodedLoopbackLiterals() {
        // 2130706433 == 0x7f000001 == 127.0.0.1
        assertFalse(isProxyUrlAllowed("http://2130706433/"))
        assertFalse(isProxyUrlAllowed("http://0x7f000001/"))
        // 0xa000005 == 10.0.0.5 (private)
        assertFalse(isProxyUrlAllowed("http://0xA000005/"))
    }

    @Test
    fun allowsIntegerEncodedPublicLiteral() {
        // 134744072 == 8.8.8.8 (public)
        assertTrue(isProxyUrlAllowed("http://134744072/"))
    }

    @Test
    fun rejectsEmptyOrMalformed() {
        assertFalse(isProxyUrlAllowed(""))
        assertFalse(isProxyUrlAllowed("https://"))
        assertFalse(isProxyUrlAllowed("not a url"))
    }
}
