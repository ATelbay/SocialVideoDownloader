package com.socialvideodownloader.shared.feature.download

import com.socialvideodownloader.shared.data.platform.DownloadErrorType
import com.socialvideodownloader.shared.network.auth.SupportedPlatform
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

/**
 * Tests for auth-error mapping in [SharedDownloadViewModel].
 *
 * `mapErrorToType` is private and exercised indirectly through the public
 * `extractWithRetry` flow triggered by [DownloadIntent.ExtractClicked].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SharedDownloadViewModelAuthTest {
    @Test
    fun authError_onSupportedPlatform_emitsAuthRequiredWithPlatform() =
        runTest {
            val vm =
                makeVm(
                    this,
                    FakeVideoExtractorRepository.alwaysFails("[Instagram] This content requires login. Sign in to continue."),
                )

            vm.onIntent(DownloadIntent.UrlChanged("https://www.instagram.com/reel/xxx"))
            vm.onIntent(DownloadIntent.ExtractClicked)
            advanceUntilIdle()

            val state = vm.uiState.value
            vm.cleanup()

            assertIs<DownloadUiState.Error>(state)
            assertEquals(DownloadErrorType.AUTH_REQUIRED, state.errorType)
            assertEquals(SupportedPlatform.INSTAGRAM, state.platformForAuth)
        }

    @Test
    fun authError_onUnsupportedPlatform_doesNotEmitAuthRequired() =
        runTest {
            // tiktok.com is not a SupportedPlatform — no cookie login flow is available.
            val vm = makeVm(this, FakeVideoExtractorRepository.alwaysFails("sign in required to view this content"))

            vm.onIntent(DownloadIntent.UrlChanged("https://tiktok.com/xxx"))
            vm.onIntent(DownloadIntent.ExtractClicked)
            advanceUntilIdle()

            val state = vm.uiState.value
            vm.cleanup()

            assertIs<DownloadUiState.Error>(state)
            assertNotEquals(DownloadErrorType.AUTH_REQUIRED, state.errorType)
            assertNull(state.platformForAuth)
        }

    @Test
    fun authError_withStoredCookies_marksReconnect() =
        runTest {
            val vm =
                makeVm(
                    this,
                    FakeVideoExtractorRepository.alwaysFails("[Instagram] Sign in to continue watching this reel."),
                    cookieStore = FakeCookieStore(connected = setOf(SupportedPlatform.INSTAGRAM)),
                )

            vm.onIntent(DownloadIntent.UrlChanged("https://www.instagram.com/reel/zzz"))
            vm.onIntent(DownloadIntent.ExtractClicked)
            advanceUntilIdle()

            val state = vm.uiState.value
            vm.cleanup()

            assertIs<DownloadUiState.Error>(state)
            assertEquals(DownloadErrorType.AUTH_REQUIRED, state.errorType)
            assertEquals(true, state.isReconnect)
        }

    @Test
    fun authError_withoutStoredCookies_isNotReconnect() =
        runTest {
            val vm =
                makeVm(
                    this,
                    FakeVideoExtractorRepository.alwaysFails("[Instagram] Sign in to continue watching this reel."),
                )

            vm.onIntent(DownloadIntent.UrlChanged("https://www.instagram.com/reel/zzz"))
            vm.onIntent(DownloadIntent.ExtractClicked)
            advanceUntilIdle()

            val state = vm.uiState.value
            vm.cleanup()

            assertIs<DownloadUiState.Error>(state)
            assertEquals(DownloadErrorType.AUTH_REQUIRED, state.errorType)
            assertEquals(false, state.isReconnect)
        }

    @Test
    fun nonAuthError_mapsToExtractionFailed() =
        runTest {
            val vm = makeVm(this, FakeVideoExtractorRepository.alwaysFails("Video unavailable"))

            vm.onIntent(DownloadIntent.UrlChanged("https://www.youtube.com/watch?v=xxx"))
            vm.onIntent(DownloadIntent.ExtractClicked)
            advanceUntilIdle()

            val state = vm.uiState.value
            vm.cleanup()

            assertIs<DownloadUiState.Error>(state)
            assertEquals(DownloadErrorType.EXTRACTION_FAILED, state.errorType)
            assertNull(state.platformForAuth)
        }

    @Test
    fun unsupportedUrlError_mapsToUnsupportedUrl() =
        runTest {
            val vm = makeVm(this, FakeVideoExtractorRepository.alwaysFails("Unsupported URL: https://example.com/bad"))

            vm.onIntent(DownloadIntent.UrlChanged("https://www.youtube.com/watch?v=xxx"))
            vm.onIntent(DownloadIntent.ExtractClicked)
            advanceUntilIdle()

            val state = vm.uiState.value
            vm.cleanup()

            assertIs<DownloadUiState.Error>(state)
            assertEquals(DownloadErrorType.UNSUPPORTED_URL, state.errorType)
        }
}
