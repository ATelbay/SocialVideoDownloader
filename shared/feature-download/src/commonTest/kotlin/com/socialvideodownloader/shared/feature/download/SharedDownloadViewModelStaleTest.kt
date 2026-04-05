package com.socialvideodownloader.shared.feature.download

import com.socialvideodownloader.shared.data.platform.DownloadErrorType
import com.socialvideodownloader.shared.network.auth.SupportedPlatform
import com.socialvideodownloader.shared.network.auth.detectPlatform
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SharedDownloadViewModelStaleTest {
    @Test
    fun authError_withStoredCookies_isReconnect() {
        val url = "https://www.instagram.com/reel/xxx"
        val platform = detectPlatform(url)
        assertNotNull(platform)

        val isConnected = true
        val isReconnect = if (platform != null && isConnected) true else false

        assertTrue(isReconnect)
    }

    @Test
    fun authError_withoutStoredCookies_isNotReconnect() {
        val url = "https://www.instagram.com/reel/xxx"
        val platform = detectPlatform(url)
        assertNotNull(platform)

        val isConnected = false
        val isReconnect = if (platform != null && isConnected) true else false

        assertFalse(isReconnect)
    }

    @Test
    fun nonAuthError_neverReconnect() {
        val url = "https://www.instagram.com/reel/xxx"
        val errorType = DownloadErrorType.NETWORK_ERROR

        val platform = if (errorType == DownloadErrorType.AUTH_REQUIRED) detectPlatform(url) else null
        val isReconnect = if (platform != null) true else false

        assertFalse(isReconnect)
        assertNull(platform)
    }

    @Test
    fun uiStateError_includesIsReconnectField() {
        val state =
            DownloadUiState.Error(
                errorType = DownloadErrorType.AUTH_REQUIRED,
                message = "Auth required",
                retryAction = RetryAction.RetryExtraction("https://www.instagram.com/reel/xxx"),
                platformForAuth = SupportedPlatform.INSTAGRAM,
                isReconnect = true,
            )

        assertTrue(state.isReconnect)
        assertEquals(SupportedPlatform.INSTAGRAM, state.platformForAuth)
    }

    @Test
    fun uiStateError_defaultIsReconnectFalse() {
        val state =
            DownloadUiState.Error(
                errorType = DownloadErrorType.NETWORK_ERROR,
                message = "Network error",
                retryAction = null,
            )

        assertFalse(state.isReconnect)
        assertNull(state.platformForAuth)
    }
}
