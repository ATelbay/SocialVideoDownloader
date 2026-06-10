package com.socialvideodownloader.shared.feature.download

import com.socialvideodownloader.shared.data.platform.DownloadErrorType
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Direct unit coverage of [DownloadErrorClassifier] keyword heuristics, decoupled
 * from the ViewModel flow that previously exercised them only indirectly.
 */
class DownloadErrorClassifierTest {
    private val youtube = "https://www.youtube.com/watch?v=abc"
    private val instagram = "https://www.instagram.com/reel/xyz"

    private fun classify(
        message: String,
        url: String = youtube,
    ): DownloadErrorType = DownloadErrorClassifier.classify(Exception(message), url)

    @Test
    fun nullMessage_mapsToUnknown() {
        assertEquals(DownloadErrorType.UNKNOWN, DownloadErrorClassifier.classify(Exception(), youtube))
    }

    @Test
    fun strongAuthKeyword_onSupportedPlatform_mapsToAuthRequired() {
        assertEquals(DownloadErrorType.AUTH_REQUIRED, classify("Please sign in to continue", instagram))
    }

    @Test
    fun authKeyword_onUnsupportedPlatform_doesNotMapToAuthRequired() {
        val result = classify("sign in required", url = "https://tiktok.com/xxx")
        assertEquals(DownloadErrorType.UNKNOWN, result)
    }

    @Test
    fun fallbackAuthHint_requiresMatchingPlatformTag() {
        // "[youtube]" tag matches the URL platform AND a weak auth hint is present.
        assertEquals(DownloadErrorType.AUTH_REQUIRED, classify("[youtube] members only content"))
    }

    @Test
    fun genericUnavailable_onYoutube_isExtractionFailedNotAuth() {
        // No auth hint → must not be misread as a login wall.
        assertEquals(DownloadErrorType.EXTRACTION_FAILED, classify("[youtube] Video unavailable"))
    }

    @Test
    fun unsupportedUrl_mapsToUnsupportedUrl() {
        assertEquals(DownloadErrorType.UNSUPPORTED_URL, classify("Unsupported URL: https://example.com"))
    }

    @Test
    fun copyrightKeyword_mapsToCopyright_notExtractionFailed() {
        assertEquals(
            DownloadErrorType.COPYRIGHT,
            classify("This video is no longer available due to a copyright claim"),
        )
    }

    @Test
    fun serverErrorCodes_mapToServerUnavailable_beforeGenericUnavailable() {
        assertEquals(DownloadErrorType.SERVER_UNAVAILABLE, classify("HTTP 503 server error"))
        assertEquals(DownloadErrorType.SERVER_UNAVAILABLE, classify("Connection refused"))
    }

    @Test
    fun networkKeywords_mapToNetworkError() {
        assertEquals(DownloadErrorType.NETWORK_ERROR, classify("connection timed out"))
        assertEquals(DownloadErrorType.NETWORK_ERROR, classify("no internet"))
    }

    @Test
    fun storageKeywords_mapToStorageFull() {
        assertEquals(DownloadErrorType.STORAGE_FULL, classify("not enough storage space"))
    }

    @Test
    fun unrecognizedMessage_mapsToUnknown() {
        assertEquals(DownloadErrorType.UNKNOWN, classify("something weird happened"))
    }
}
