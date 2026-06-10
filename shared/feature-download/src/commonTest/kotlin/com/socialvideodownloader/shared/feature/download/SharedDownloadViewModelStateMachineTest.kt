package com.socialvideodownloader.shared.feature.download

import app.cash.turbine.test
import com.socialvideodownloader.core.domain.model.DownloadProgress
import com.socialvideodownloader.shared.data.platform.DownloadErrorType
import com.socialvideodownloader.shared.data.platform.DownloadServiceState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * End-to-end coverage of the [SharedDownloadViewModel] state machine: extraction
 * success/failure, format selection, the download service-state transitions, retry
 * with backoff, and background-download isolation.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SharedDownloadViewModelStateMachineTest {
    private val url = "https://www.youtube.com/watch?v=abc"

    // ------------------------------------------------------------------
    // Extraction → FormatSelection
    // ------------------------------------------------------------------

    @Test
    fun extractSuccess_transitionsToFormatSelection_andSelectsFirstVideoFormat() =
        runTest {
            val metadata =
                videoMetadata(
                    formats =
                        listOf(
                            videoFormat("v1080", resolution = 1080),
                            videoFormat("v720", resolution = 720),
                            videoFormat("audio", isAudioOnly = true),
                        ),
                )
            val vm = makeVm(this, FakeVideoExtractorRepository.alwaysSucceeds(metadata))

            vm.onIntent(DownloadIntent.UrlChanged(url))
            vm.onIntent(DownloadIntent.ExtractClicked)
            advanceUntilIdle()

            val state = vm.uiState.value
            vm.cleanup()

            assertIs<DownloadUiState.FormatSelection>(state)
            // First non-audio format wins (formats are expected pre-sorted best-first).
            assertEquals("v1080", state.selectedFormatId)
        }

    @Test
    fun extractSuccess_onlyAudio_selectsAudioFormat() =
        runTest {
            val metadata = videoMetadata(formats = listOf(videoFormat("audio", isAudioOnly = true)))
            val vm = makeVm(this, FakeVideoExtractorRepository.alwaysSucceeds(metadata))

            vm.onIntent(DownloadIntent.UrlChanged(url))
            vm.onIntent(DownloadIntent.ExtractClicked)
            advanceUntilIdle()

            val state = vm.uiState.value
            vm.cleanup()

            assertIs<DownloadUiState.FormatSelection>(state)
            assertEquals("audio", state.selectedFormatId)
        }

    @Test
    fun extractSuccess_withNoFormats_transitionsToExtractionFailedError() =
        runTest {
            val metadata = videoMetadata(formats = emptyList())
            val vm = makeVm(this, FakeVideoExtractorRepository.alwaysSucceeds(metadata))

            vm.onIntent(DownloadIntent.UrlChanged(url))
            vm.onIntent(DownloadIntent.ExtractClicked)
            advanceUntilIdle()

            val state = vm.uiState.value
            vm.cleanup()

            assertIs<DownloadUiState.Error>(state)
            assertEquals(DownloadErrorType.EXTRACTION_FAILED, state.errorType)
        }

    @Test
    fun formatSelected_updatesSelectedFormatId() =
        runTest {
            val metadata =
                videoMetadata(
                    formats = listOf(videoFormat("v1080", resolution = 1080), videoFormat("v720", resolution = 720)),
                )
            val vm = makeVm(this, FakeVideoExtractorRepository.alwaysSucceeds(metadata))

            vm.onIntent(DownloadIntent.UrlChanged(url))
            vm.onIntent(DownloadIntent.ExtractClicked)
            advanceUntilIdle()
            vm.onIntent(DownloadIntent.FormatSelected("v720"))

            val state = vm.uiState.value
            vm.cleanup()

            assertIs<DownloadUiState.FormatSelection>(state)
            assertEquals("v720", state.selectedFormatId)
        }

    // ------------------------------------------------------------------
    // FormatSelection → Downloading → terminal states
    // ------------------------------------------------------------------

    @Test
    fun download_started_transitionsToDownloading_andDispatchesRequest() =
        runTest {
            val manager = ControllablePlatformDownloadManager()
            val vm = driveToDownloading(manager)

            val state = vm.uiState.value
            assertIs<DownloadUiState.Downloading>(state)
            assertFalse(state.isShareMode)
            assertEquals(1, manager.startedRequests.size)
            assertEquals(state.progress.requestId, manager.startedRequests.first().id)
            vm.cleanup()
        }

    @Test
    fun download_progressUpdate_updatesDownloadingProgress() =
        runTest {
            val manager = ControllablePlatformDownloadManager()
            val vm = driveToDownloading(manager)
            val requestId = (vm.uiState.value as DownloadUiState.Downloading).progress.requestId

            manager.emit(
                DownloadServiceState.Downloading(
                    requestId = requestId,
                    progress = progress(requestId, percent = 42f),
                ),
            )
            advanceUntilIdle()

            val state = vm.uiState.value
            vm.cleanup()
            assertIs<DownloadUiState.Downloading>(state)
            assertEquals(42f, state.progress.progressPercent)
        }

    @Test
    fun download_completed_transitionsToDone() =
        runTest {
            val manager = ControllablePlatformDownloadManager()
            val vm = driveToDownloading(manager)
            val requestId = (vm.uiState.value as DownloadUiState.Downloading).progress.requestId

            manager.emit(
                DownloadServiceState.Completed(
                    requestId = requestId,
                    filePath = "/path/video.mp4",
                    fileUri = "content://video",
                ),
            )
            advanceUntilIdle()

            val state = vm.uiState.value
            vm.cleanup()
            assertIs<DownloadUiState.Done>(state)
            assertEquals("/path/video.mp4", state.filePath)
            assertEquals("content://video", state.fileUri)
        }

    @Test
    fun download_failed_transitionsToErrorWithServiceErrorType() =
        runTest {
            val manager = ControllablePlatformDownloadManager()
            val vm = driveToDownloading(manager)
            val requestId = (vm.uiState.value as DownloadUiState.Downloading).progress.requestId

            manager.emit(DownloadServiceState.Failed(requestId = requestId, error = DownloadErrorType.STORAGE_FULL))
            advanceUntilIdle()

            val state = vm.uiState.value
            vm.cleanup()
            assertIs<DownloadUiState.Error>(state)
            assertEquals(DownloadErrorType.STORAGE_FULL, state.errorType)
        }

    @Test
    fun download_cancelled_returnsToFormatSelection() =
        runTest {
            val manager = ControllablePlatformDownloadManager()
            val vm = driveToDownloading(manager)
            val requestId = (vm.uiState.value as DownloadUiState.Downloading).progress.requestId

            vm.onIntent(DownloadIntent.CancelDownloadClicked)
            assertEquals(listOf(requestId), manager.cancelledIds)

            manager.emit(DownloadServiceState.Cancelled(requestId = requestId))
            advanceUntilIdle()

            val state = vm.uiState.value
            vm.cleanup()
            assertIs<DownloadUiState.FormatSelection>(state)
        }

    @Test
    fun shareDownload_completed_emitsShareEvent_andReturnsToFormatSelection() =
        runTest {
            val manager = ControllablePlatformDownloadManager()
            val vm = driveToFormatSelection(manager)

            vm.events.test {
                vm.onIntent(DownloadIntent.ShareFormatClicked)
                advanceUntilIdle()
                val downloading = vm.uiState.value
                assertIs<DownloadUiState.Downloading>(downloading)
                assertTrue(downloading.isShareMode)

                manager.emit(
                    DownloadServiceState.Completed(
                        requestId = downloading.progress.requestId,
                        filePath = "/path/share.mp4",
                        fileUri = "content://share",
                    ),
                )
                advanceUntilIdle()

                val event = awaitItem()
                assertIs<DownloadEvent.ShareFile>(event)
                assertEquals("content://share", event.filePath)
                cancelAndIgnoreRemainingEvents()
            }

            // Share mode returns to FormatSelection rather than Done.
            assertIs<DownloadUiState.FormatSelection>(vm.uiState.value)
            vm.cleanup()
        }

    // ------------------------------------------------------------------
    // Retry / backoff
    //
    // NOTE: SharedDownloadViewModel.isCurrentExtraction() guards onSuccess /
    // terminal-onFailure so a superseded extraction can't clobber the live state.
    // That race only manifests on a real Main.immediate dispatcher — there is no
    // suspension point between the network return and the state write, so a
    // cancelled continuation still runs. On the single-threaded TestDispatcher used
    // here, cancellation interrupts cleanly at the fake's suspension point, so the
    // race is not reproducible and the guard has no dedicated test. It is defensive.
    // ------------------------------------------------------------------

    @Test
    fun transientError_retriesThenSucceeds() =
        runTest {
            val metadata = videoMetadata(formats = listOf(videoFormat("v720")))
            // Fail twice with a network error, then succeed on the third attempt.
            val extractor =
                FakeVideoExtractorRepository { _, callIndex ->
                    if (callIndex < 2) throw Exception("network error: connection timed out")
                    metadata
                }
            val vm = makeVm(this, extractor)

            vm.onIntent(DownloadIntent.UrlChanged(url))
            vm.onIntent(DownloadIntent.ExtractClicked)
            advanceUntilIdle()

            val state = vm.uiState.value
            vm.cleanup()
            assertIs<DownloadUiState.FormatSelection>(state)
            assertEquals(3, extractor.extractCount)
        }

    @Test
    fun transientError_exhaustsRetries_thenError() =
        runTest {
            val extractor = FakeVideoExtractorRepository.alwaysFails("network error: connection timed out")
            val vm = makeVm(this, extractor)

            vm.onIntent(DownloadIntent.UrlChanged(url))
            vm.onIntent(DownloadIntent.ExtractClicked)
            advanceUntilIdle()

            val state = vm.uiState.value
            vm.cleanup()
            assertIs<DownloadUiState.Error>(state)
            assertEquals(DownloadErrorType.NETWORK_ERROR, state.errorType)
            // 1 initial attempt + 3 retries.
            assertEquals(4, extractor.extractCount)
        }

    @Test
    fun permanentError_isNotRetried() =
        runTest {
            val extractor = FakeVideoExtractorRepository.alwaysFails("Unsupported URL: https://example.com")
            val vm = makeVm(this, extractor)

            vm.onIntent(DownloadIntent.UrlChanged(url))
            vm.onIntent(DownloadIntent.ExtractClicked)
            advanceUntilIdle()

            val state = vm.uiState.value
            vm.cleanup()
            assertIs<DownloadUiState.Error>(state)
            assertEquals(DownloadErrorType.UNSUPPORTED_URL, state.errorType)
            assertEquals(1, extractor.extractCount)
        }

    @Test
    fun retryClicked_fromError_reExtracts() =
        runTest {
            val metadata = videoMetadata(formats = listOf(videoFormat("v720")))
            val extractor =
                FakeVideoExtractorRepository { _, callIndex ->
                    // First full extract (1 try) fails permanently; after Retry it succeeds.
                    if (callIndex == 0) throw Exception("Unsupported URL") else metadata
                }
            val vm = makeVm(this, extractor)

            vm.onIntent(DownloadIntent.UrlChanged(url))
            vm.onIntent(DownloadIntent.ExtractClicked)
            advanceUntilIdle()
            assertIs<DownloadUiState.Error>(vm.uiState.value)

            vm.onIntent(DownloadIntent.RetryClicked)
            advanceUntilIdle()

            val state = vm.uiState.value
            vm.cleanup()
            assertIs<DownloadUiState.FormatSelection>(state)
        }

    // ------------------------------------------------------------------
    // Background-download isolation (regression for terminal-event clobber)
    // ------------------------------------------------------------------

    @Test
    fun backgroundCompletion_doesNotClobberActiveNewDownload() =
        runTest {
            val manager = ControllablePlatformDownloadManager()
            val vm = driveToDownloading(manager)
            val firstRequestId = (vm.uiState.value as DownloadUiState.Downloading).progress.requestId

            // User starts a brand-new download flow; the first one keeps running in the background.
            vm.onIntent(DownloadIntent.NewDownloadClicked)
            assertIs<DownloadUiState.Idle>(vm.uiState.value)

            vm.onIntent(DownloadIntent.UrlChanged(url))
            vm.onIntent(DownloadIntent.ExtractClicked)
            advanceUntilIdle()
            vm.onIntent(DownloadIntent.DownloadClicked)
            advanceUntilIdle()
            val secondState = vm.uiState.value
            assertIs<DownloadUiState.Downloading>(secondState)
            val secondRequestId = secondState.progress.requestId
            assertTrue(firstRequestId != secondRequestId)

            // The old background download completes — must NOT overwrite the active new download.
            manager.emit(
                DownloadServiceState.Completed(
                    requestId = firstRequestId,
                    filePath = "/path/old.mp4",
                ),
            )
            advanceUntilIdle()

            val state = vm.uiState.value
            vm.cleanup()
            assertIs<DownloadUiState.Downloading>(state)
            assertEquals(secondRequestId, state.progress.requestId)
        }

    @Test
    fun backgroundCompletion_whenIdle_isSurfacedAsDone() =
        runTest {
            val manager = ControllablePlatformDownloadManager()
            val vm = driveToDownloading(manager)
            val requestId = (vm.uiState.value as DownloadUiState.Downloading).progress.requestId

            vm.onIntent(DownloadIntent.NewDownloadClicked)
            assertIs<DownloadUiState.Idle>(vm.uiState.value)

            manager.emit(
                DownloadServiceState.Completed(
                    requestId = requestId,
                    filePath = "/path/bg.mp4",
                    fileUri = "content://bg",
                ),
            )
            advanceUntilIdle()

            val state = vm.uiState.value
            vm.cleanup()
            assertIs<DownloadUiState.Done>(state)
            assertEquals("/path/bg.mp4", state.filePath)
        }

    @Test
    fun backgroundFailure_whenMidAnotherFlow_surfacesErrorEvent_withoutClobbering() =
        runTest {
            val manager = ControllablePlatformDownloadManager()
            val vm = driveToDownloading(manager)
            val firstRequestId = (vm.uiState.value as DownloadUiState.Downloading).progress.requestId

            // Background the first download and move into a fresh flow (current != Idle).
            vm.onIntent(DownloadIntent.NewDownloadClicked)
            vm.onIntent(DownloadIntent.UrlChanged(url))
            vm.onIntent(DownloadIntent.ExtractClicked)
            advanceUntilIdle()
            assertIs<DownloadUiState.FormatSelection>(vm.uiState.value)

            vm.events.test {
                // The backgrounded download fails — must surface an error event, not vanish.
                manager.emit(
                    DownloadServiceState.Failed(
                        requestId = firstRequestId,
                        error = DownloadErrorType.NETWORK_ERROR,
                    ),
                )
                advanceUntilIdle()

                val event = awaitItem()
                assertIs<DownloadEvent.ShowError>(event)
                assertEquals(DownloadErrorType.NETWORK_ERROR, event.errorType)
                cancelAndIgnoreRemainingEvents()
            }

            // Active new flow must be untouched by the background failure.
            assertIs<DownloadUiState.FormatSelection>(vm.uiState.value)
            vm.cleanup()
        }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private fun progress(
        requestId: String,
        percent: Float,
    ): DownloadProgress =
        DownloadProgress(
            requestId = requestId,
            progressPercent = percent,
            downloadedBytes = 0L,
            totalBytes = 1_000_000L,
            speedBytesPerSec = 0L,
            etaSeconds = 0L,
        )

    private fun kotlinx.coroutines.test.TestScope.driveToFormatSelection(
        manager: ControllablePlatformDownloadManager,
    ): SharedDownloadViewModel {
        val metadata = videoMetadata(formats = listOf(videoFormat("v720")))
        val vm = makeVm(this, FakeVideoExtractorRepository.alwaysSucceeds(metadata), downloadManager = manager)
        vm.onIntent(DownloadIntent.UrlChanged(url))
        vm.onIntent(DownloadIntent.ExtractClicked)
        advanceUntilIdle()
        return vm
    }

    private fun kotlinx.coroutines.test.TestScope.driveToDownloading(
        manager: ControllablePlatformDownloadManager,
    ): SharedDownloadViewModel {
        val vm = driveToFormatSelection(manager)
        vm.onIntent(DownloadIntent.DownloadClicked)
        advanceUntilIdle()
        return vm
    }
}
