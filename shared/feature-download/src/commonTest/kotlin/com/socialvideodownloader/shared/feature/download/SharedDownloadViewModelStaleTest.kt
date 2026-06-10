package com.socialvideodownloader.shared.feature.download

import com.socialvideodownloader.shared.network.auth.SupportedPlatform
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Connected-platform lifecycle: the [DownloadUiState.Idle.connectedPlatforms]
 * list must reflect the cookie store, and disconnect must update it in place.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SharedDownloadViewModelStaleTest {
    private val extractor = FakeVideoExtractorRepository.alwaysFails("unused")

    @Test
    fun idle_reflectsConnectedPlatformsFromCookieStore() =
        runTest {
            val vm =
                makeVm(
                    this,
                    extractor,
                    cookieStore = FakeCookieStore(connected = setOf(SupportedPlatform.INSTAGRAM)),
                )
            advanceUntilIdle()

            val state = vm.uiState.value
            vm.cleanup()

            assertIs<DownloadUiState.Idle>(state)
            assertTrue(SupportedPlatform.INSTAGRAM in state.connectedPlatforms)
        }

    @Test
    fun disconnectPlatform_removesItFromConnectedPlatforms() =
        runTest {
            val vm =
                makeVm(
                    this,
                    extractor,
                    cookieStore =
                        FakeCookieStore(
                            connected = setOf(SupportedPlatform.INSTAGRAM, SupportedPlatform.YOUTUBE),
                        ),
                )
            advanceUntilIdle()

            vm.onIntent(DownloadIntent.DisconnectPlatformClicked(SupportedPlatform.INSTAGRAM))

            val state = vm.uiState.value
            vm.cleanup()

            assertIs<DownloadUiState.Idle>(state)
            assertEquals(listOf(SupportedPlatform.YOUTUBE), state.connectedPlatforms)
        }
}
