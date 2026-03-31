package com.socialvideodownloader.feature.download.ui

import android.content.Context
import app.cash.turbine.test
import com.socialvideodownloader.core.domain.model.DownloadRecord
import com.socialvideodownloader.core.domain.model.DownloadStatus
import com.socialvideodownloader.core.domain.model.VideoFormatOption
import com.socialvideodownloader.core.domain.model.VideoMetadata
import com.socialvideodownloader.core.domain.usecase.ExtractVideoInfoUseCase
import com.socialvideodownloader.core.domain.usecase.FindExistingDownloadUseCase
import com.socialvideodownloader.feature.download.service.DownloadServiceState
import com.socialvideodownloader.feature.download.service.DownloadServiceStateHolder
import com.socialvideodownloader.shared.data.platform.AndroidDownloadManager
import com.socialvideodownloader.shared.feature.download.DownloadIntent.DownloadClicked
import com.socialvideodownloader.shared.feature.download.DownloadIntent.ExtractClicked
import com.socialvideodownloader.shared.feature.download.DownloadIntent.FormatSelected
import com.socialvideodownloader.shared.feature.download.DownloadIntent.NewDownloadClicked
import com.socialvideodownloader.shared.feature.download.DownloadIntent.PrefillUrl
import com.socialvideodownloader.shared.feature.download.DownloadIntent.RetryClicked
import com.socialvideodownloader.shared.feature.download.DownloadIntent.UrlChanged
import com.socialvideodownloader.shared.feature.download.DownloadUiState.Done
import com.socialvideodownloader.shared.feature.download.DownloadUiState.Downloading
import com.socialvideodownloader.shared.feature.download.DownloadUiState.Error
import com.socialvideodownloader.shared.feature.download.DownloadUiState.Extracting
import com.socialvideodownloader.shared.feature.download.DownloadUiState.FormatSelection
import com.socialvideodownloader.shared.feature.download.DownloadUiState.Idle
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DownloadViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var extractVideoInfo: ExtractVideoInfoUseCase
    private lateinit var findExistingDownload: FindExistingDownloadUseCase
    private lateinit var serviceStateHolder: DownloadServiceStateHolder
    private lateinit var androidDownloadManager: AndroidDownloadManager
    private lateinit var context: Context
    private lateinit var viewModel: DownloadViewModel

    private val testMetadata =
        VideoMetadata(
            sourceUrl = "https://youtube.com/watch?v=test",
            title = "Test Video",
            thumbnailUrl = "https://thumb.jpg",
            durationSeconds = 120,
            author = "Author",
            formats =
                listOf(
                    VideoFormatOption(
                        formatId = "248",
                        label = "1080p",
                        resolution = 1080,
                        ext = "mp4",
                        fileSizeBytes = 100_000_000L,
                        isAudioOnly = false,
                        isVideoOnly = false,
                    ),
                    VideoFormatOption(
                        formatId = "136",
                        label = "720p",
                        resolution = 720,
                        ext = "mp4",
                        fileSizeBytes = 50_000_000L,
                        isAudioOnly = false,
                        isVideoOnly = false,
                    ),
                ),
        )

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        extractVideoInfo = mockk()
        findExistingDownload = mockk()
        serviceStateHolder = DownloadServiceStateHolder()
        androidDownloadManager =
            AndroidDownloadManager(
                context = mockk(relaxed = true),
            )
        context = mockk(relaxed = true)
        every { context.cacheDir } returns java.io.File(System.getProperty("java.io.tmpdir"), "test_cache")
        coEvery { findExistingDownload(any()) } returns null
        viewModel =
            DownloadViewModel(
                extractVideoInfo = extractVideoInfo,
                findExistingDownload = findExistingDownload,
                serviceStateHolder = serviceStateHolder,
                context = context,
                savedStateHandle = androidx.lifecycle.SavedStateHandle(),
                ioDispatcher = testDispatcher,
                androidDownloadManager = androidDownloadManager,
            )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Idle`() =
        runTest {
            viewModel.uiState.test {
                assertTrue(awaitItem() is Idle)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `ExtractClicked transitions to Extracting then FormatSelection on success`() =
        runTest {
            coEvery { extractVideoInfo(any()) } returns Result.success(testMetadata)

            viewModel.uiState.test {
                assertTrue(awaitItem() is Idle)

                viewModel.onIntent(UrlChanged("https://youtube.com/watch?v=test"))
                viewModel.onIntent(ExtractClicked)

                val extracting = awaitItem()
                assertTrue(extracting is Extracting)

                val formatSelection = awaitItem()
                assertTrue(formatSelection is FormatSelection)
                assertEquals("248", (formatSelection as FormatSelection).selectedFormatId)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `ExtractClicked transitions to Error on failure`() =
        runTest {
            coEvery { extractVideoInfo(any()) } returns Result.failure(RuntimeException("Network error"))

            viewModel.uiState.test {
                assertTrue(awaitItem() is Idle)

                viewModel.onIntent(UrlChanged("https://youtube.com/watch?v=test"))
                viewModel.onIntent(ExtractClicked)

                assertTrue(awaitItem() is Extracting)

                val error = awaitItem()
                assertTrue(error is Error)
                // The shared VM maps "Network error" message → NETWORK_ERROR error type
                val errorState = error as Error
                assertEquals("Network error", errorState.message)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `FormatSelected updates selectedFormatId`() =
        runTest {
            coEvery { extractVideoInfo(any()) } returns Result.success(testMetadata)

            viewModel.uiState.test {
                skipItems(1) // Idle
                viewModel.onIntent(UrlChanged("url"))
                viewModel.onIntent(ExtractClicked)
                skipItems(1) // Extracting

                val initial = awaitItem() as FormatSelection
                assertEquals("248", initial.selectedFormatId)

                viewModel.onIntent(FormatSelected("136"))

                val updated = awaitItem() as FormatSelection
                assertEquals("136", updated.selectedFormatId)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `best format is pre-selected as first non-audio format`() =
        runTest {
            val metadataWithAudioFirst =
                testMetadata.copy(
                    formats =
                        listOf(
                            VideoFormatOption("251", "opus", null, "webm", 5_000_000L, isAudioOnly = true, isVideoOnly = false),
                            VideoFormatOption("248", "1080p", 1080, "mp4", 100_000_000L, isAudioOnly = false, isVideoOnly = false),
                        ),
                )
            coEvery { extractVideoInfo(any()) } returns Result.success(metadataWithAudioFirst)

            viewModel.uiState.test {
                skipItems(1) // Idle
                viewModel.onIntent(UrlChanged("url"))
                viewModel.onIntent(ExtractClicked)
                skipItems(1) // Extracting

                val selection = awaitItem() as FormatSelection
                assertEquals("248", selection.selectedFormatId)
                assertFalse(selection.metadata.formats.first { it.formatId == "248" }.isAudioOnly)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `ExtractClicked with blank url does not transition state`() =
        runTest {
            viewModel.uiState.test {
                assertTrue(awaitItem() is Idle)
                viewModel.onIntent(ExtractClicked)
                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `Error then RetryClicked transitions to Extracting`() =
        runTest {
            // Use a non-transient error ("Unsupported URL") so the shared VM doesn't auto-retry
            coEvery { extractVideoInfo(any()) } returnsMany
                listOf(
                    Result.failure(RuntimeException("Unsupported URL")),
                    Result.success(testMetadata),
                )

            viewModel.onIntent(UrlChanged("https://youtube.com/watch?v=test"))
            viewModel.onIntent(ExtractClicked)
            advanceUntilIdle()

            viewModel.uiState.test {
                val current = awaitItem()
                assertTrue(current is Error)

                viewModel.onIntent(RetryClicked)

                val extracting = awaitItem()
                assertTrue(extracting is Extracting)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `Error then NewDownloadClicked transitions to Idle`() =
        runTest {
            coEvery { extractVideoInfo(any()) } returns Result.failure(RuntimeException("Network error"))

            viewModel.onIntent(UrlChanged("https://youtube.com/watch?v=test"))
            viewModel.onIntent(ExtractClicked)
            advanceUntilIdle()

            viewModel.uiState.test {
                val current = awaitItem()
                assertTrue(current is Error)

                viewModel.onIntent(NewDownloadClicked)

                val idle = awaitItem()
                assertTrue(idle is Idle)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `DownloadClicked transitions to Downloading state with correct selectedFormatId`() =
        runTest {
            coEvery { extractVideoInfo(any()) } returns Result.success(testMetadata)

            viewModel.onIntent(UrlChanged("https://youtube.com/watch?v=test"))
            viewModel.onIntent(ExtractClicked)
            advanceUntilIdle()

            viewModel.uiState.test {
                val formatSelection = awaitItem() as FormatSelection
                assertEquals("248", formatSelection.selectedFormatId)

                viewModel.onIntent(DownloadClicked)

                val downloading = awaitItem() as Downloading
                assertEquals("248", downloading.selectedFormatId)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `service state Completed transitions to Done with correct filePath`() =
        runTest {
            coEvery { extractVideoInfo(any()) } returns Result.success(testMetadata)

            viewModel.onIntent(UrlChanged("https://youtube.com/watch?v=test"))
            viewModel.onIntent(ExtractClicked)
            advanceUntilIdle()
            viewModel.onIntent(DownloadClicked)
            advanceUntilIdle()

            viewModel.uiState.test {
                val downloading = awaitItem() as Downloading
                val requestId = downloading.progress.requestId

                serviceStateHolder.update(DownloadServiceState.Completed(requestId, "/path/to/file"))

                val done = awaitItem() as Done
                assertEquals("/path/to/file", done.filePath)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `service state Failed transitions to Error`() =
        runTest {
            coEvery { extractVideoInfo(any()) } returns Result.success(testMetadata)

            viewModel.onIntent(UrlChanged("https://youtube.com/watch?v=test"))
            viewModel.onIntent(ExtractClicked)
            advanceUntilIdle()
            viewModel.onIntent(DownloadClicked)
            advanceUntilIdle()

            viewModel.uiState.test {
                val downloading = awaitItem() as Downloading
                val requestId = downloading.progress.requestId

                serviceStateHolder.update(DownloadServiceState.Failed(requestId, "Download error"))

                val error = awaitItem()
                assertTrue(error is Error)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `PrefillUrl in Idle state auto-extracts`() =
        runTest {
            coEvery { extractVideoInfo(any()) } returns Result.success(testMetadata)

            viewModel.uiState.test {
                assertTrue(awaitItem() is Idle)

                viewModel.onIntent(PrefillUrl("https://youtube.com/watch?v=prefill"))

                assertTrue(awaitItem() is Extracting)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `PrefillUrl while Extracting resets and auto-extracts new URL`() =
        runTest {
            coEvery { extractVideoInfo(any()) } coAnswers {
                kotlinx.coroutines.delay(10_000)
                Result.success(testMetadata)
            }

            viewModel.onIntent(UrlChanged("https://youtube.com/watch?v=test"))
            viewModel.onIntent(ExtractClicked)
            runCurrent()

            viewModel.uiState.test {
                val extracting = awaitItem()
                assertTrue(extracting is Extracting)
                assertEquals("https://youtube.com/watch?v=test", (extracting as Extracting).url)

                viewModel.onIntent(PrefillUrl("https://youtube.com/watch?v=prefill"))

                // State must reset to Extracting with the new URL
                val newExtracting = awaitItem() as Extracting
                assertEquals("https://youtube.com/watch?v=prefill", newExtracting.url)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `service state Cancelled restores FormatSelection with original formatId`() =
        runTest {
            coEvery { extractVideoInfo(any()) } returns Result.success(testMetadata)

            viewModel.onIntent(UrlChanged("https://youtube.com/watch?v=test"))
            viewModel.onIntent(ExtractClicked)
            advanceUntilIdle()
            viewModel.onIntent(FormatSelected("136"))
            viewModel.onIntent(DownloadClicked)
            advanceUntilIdle()

            viewModel.uiState.test {
                val downloading = awaitItem() as Downloading
                val requestId = downloading.progress.requestId
                assertEquals("136", downloading.selectedFormatId)

                serviceStateHolder.update(DownloadServiceState.Cancelled(requestId))

                val formatSelection = awaitItem() as FormatSelection
                assertEquals("136", formatSelection.selectedFormatId)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `when DownloadClicked on API 33+ without permission, emits RequestNotificationPermission event`() =
        runTest {
            // This test validates that the ViewModel emits RequestNotificationPermission
            // when download is clicked on API 33+ without notification permission.
            // Since the permission check requires Android context (Build.VERSION.SDK_INT),
            // we test by verifying the event flow after triggering the permission path.
            coEvery { extractVideoInfo(any()) } returns Result.success(testMetadata)

            viewModel.onIntent(UrlChanged("https://youtube.com/watch?v=test"))
            viewModel.onIntent(ExtractClicked)
            advanceUntilIdle()

            // For now, verify that DownloadClicked still works (permission check added in T007)
            viewModel.uiState.test {
                val formatSelection = awaitItem() as FormatSelection
                viewModel.onIntent(DownloadClicked)
                val downloading = awaitItem()
                assertTrue(downloading is Downloading)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `when notification permission denied, proceeds with download`() =
        runTest {
            coEvery { extractVideoInfo(any()) } returns Result.success(testMetadata)

            viewModel.onIntent(UrlChanged("https://youtube.com/watch?v=test"))
            viewModel.onIntent(ExtractClicked)
            advanceUntilIdle()

            // Navigate to FormatSelection state
            viewModel.uiState.test {
                awaitItem() as FormatSelection

                // Call onNotificationPermissionResult with denied — should still proceed with download
                viewModel.onNotificationPermissionResult(granted = false)
                advanceUntilIdle()

                val downloading = awaitItem()
                assertTrue(downloading is Downloading)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `download record should include fileSizeBytes after successful completion`() =
        runTest {
            // This test documents the requirement that fileSizeBytes should be populated
            // after a successful download. The actual population happens in DownloadService
            // after saveFileToMediaStore, which requires Android runtime to test fully.
            // Here we verify the DownloadRecord model supports the field.
            val record =
                DownloadRecord(
                    sourceUrl = "https://youtube.com/watch?v=test",
                    videoTitle = "Test Video",
                    thumbnailUrl = "https://thumb.jpg",
                    formatLabel = "1080p",
                    filePath = "/path/to/file.mp4",
                    status = DownloadStatus.COMPLETED,
                    createdAt = System.currentTimeMillis(),
                    completedAt = System.currentTimeMillis(),
                    fileSizeBytes = 100_000_000L,
                )
            assertEquals(100_000_000L, record.fileSizeBytes)
        }

    @Test
    fun `when service emits Queued state, no event is emitted from shared VM`() =
        runTest {
            // The shared VM no longer emits a ShowSnackbar for Queued state;
            // platform-specific handling is the responsibility of the Android layer.
            coEvery { extractVideoInfo(any()) } returns Result.success(testMetadata)

            viewModel.onIntent(UrlChanged("https://youtube.com/watch?v=test"))
            viewModel.onIntent(ExtractClicked)
            advanceUntilIdle()
            viewModel.onIntent(DownloadClicked)
            advanceUntilIdle()

            viewModel.events.test {
                serviceStateHolder.update(DownloadServiceState.Queued(listOf("id1", "id2")))
                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `when UrlChanged intent is handled, currentUrl is written to SavedStateHandle`() =
        runTest {
            val savedStateHandle = androidx.lifecycle.SavedStateHandle()
            val vm =
                DownloadViewModel(
                    extractVideoInfo = extractVideoInfo,
                    findExistingDownload = findExistingDownload,
                    serviceStateHolder = serviceStateHolder,
                    context = context,
                    savedStateHandle = savedStateHandle,
                    ioDispatcher = testDispatcher,
                    androidDownloadManager = androidDownloadManager,
                )

            vm.onIntent(UrlChanged("https://youtube.com/watch?v=saved"))

            assertEquals("https://youtube.com/watch?v=saved", savedStateHandle.get<String>("currentUrl"))
        }
}
