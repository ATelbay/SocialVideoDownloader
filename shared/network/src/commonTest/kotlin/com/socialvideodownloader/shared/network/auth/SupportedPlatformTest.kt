package com.socialvideodownloader.shared.network.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SupportedPlatformTest {

    // ---- detectPlatform ----

    @Test
    fun detectPlatform_instagram_wwwSubdomain() {
        assertEquals(SupportedPlatform.INSTAGRAM, detectPlatform("https://www.instagram.com/reel/xxx"))
    }

    @Test
    fun detectPlatform_instagram_noSubdomain() {
        assertEquals(SupportedPlatform.INSTAGRAM, detectPlatform("https://instagram.com/p/yyy"))
    }

    @Test
    fun detectPlatform_youtube_www() {
        assertEquals(SupportedPlatform.YOUTUBE, detectPlatform("https://www.youtube.com/watch?v=xxx"))
    }

    @Test
    fun detectPlatform_youtube_mobile() {
        assertEquals(SupportedPlatform.YOUTUBE, detectPlatform("https://m.youtube.com/watch?v=xxx"))
    }

    @Test
    fun detectPlatform_youtubeShortLink_returnsNull() {
        // youtu.be is NOT in hostMatches for YOUTUBE
        assertNull(detectPlatform("https://youtu.be/xxx"))
    }

    @Test
    fun detectPlatform_twitter_x() {
        assertEquals(SupportedPlatform.TWITTER, detectPlatform("https://x.com/user/status/123"))
    }

    @Test
    fun detectPlatform_twitter_twitterCom() {
        assertEquals(SupportedPlatform.TWITTER, detectPlatform("https://twitter.com/user/status/123"))
    }

    @Test
    fun detectPlatform_reddit_www() {
        assertEquals(SupportedPlatform.REDDIT, detectPlatform("https://www.reddit.com/r/sub/comments/xxx"))
    }

    @Test
    fun detectPlatform_reddit_old() {
        assertEquals(SupportedPlatform.REDDIT, detectPlatform("https://old.reddit.com/r/sub/xxx"))
    }

    @Test
    fun detectPlatform_facebook_www() {
        assertEquals(SupportedPlatform.FACEBOOK, detectPlatform("https://www.facebook.com/watch/?v=xxx"))
    }

    @Test
    fun detectPlatform_facebook_mobile() {
        assertEquals(SupportedPlatform.FACEBOOK, detectPlatform("https://m.facebook.com/xxx"))
    }

    @Test
    fun detectPlatform_unknownHost_returnsNull() {
        assertNull(detectPlatform("https://example.com/video"))
    }

    @Test
    fun detectPlatform_tiktok_returnsNull() {
        // tiktok.com is not in any platform's hostMatches
        assertNull(detectPlatform("https://tiktok.com/xxx"))
    }

    @Test
    fun detectPlatform_emptyString_returnsNull() {
        assertNull(detectPlatform(""))
    }

    @Test
    fun detectPlatform_malformedUrl_returnsNull() {
        // A string with no sensible host should return null
        assertNull(detectPlatform("not a url at all ://???"))
    }

    // ---- detectPlatformFromError ----

    @Test
    fun detectPlatformFromError_instagram() {
        assertEquals(SupportedPlatform.INSTAGRAM, detectPlatformFromError("[Instagram] This content requires login"))
    }

    @Test
    fun detectPlatformFromError_youtube() {
        assertEquals(SupportedPlatform.YOUTUBE, detectPlatformFromError("[youtube] Sign in to confirm your age"))
    }

    @Test
    fun detectPlatformFromError_twitter() {
        assertEquals(SupportedPlatform.TWITTER, detectPlatformFromError("[twitter] NSFW content requires login"))
    }

    @Test
    fun detectPlatformFromError_reddit() {
        assertEquals(SupportedPlatform.REDDIT, detectPlatformFromError("[reddit] This content is NSFW"))
    }

    @Test
    fun detectPlatformFromError_facebook() {
        assertEquals(SupportedPlatform.FACEBOOK, detectPlatformFromError("[facebook] Login required"))
    }

    @Test
    fun detectPlatformFromError_twitterDisplayName() {
        // Matches "[Twitter / X]" via the displayName tag check
        assertEquals(SupportedPlatform.TWITTER, detectPlatformFromError("[Twitter / X] some error"))
    }

    @Test
    fun detectPlatformFromError_noPlatformTag_returnsNull() {
        assertNull(detectPlatformFromError("Some random error without platform"))
    }

    @Test
    fun detectPlatformFromError_emptyString_returnsNull() {
        assertNull(detectPlatformFromError(""))
    }
}
