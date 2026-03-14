package com.socialvideodownloader.feature.download.ui

import android.content.Context
import app.cash.turbine.test
import com.socialvideodownloader.core.domain.model.DownloadProgress
import com.socialvideodownloader.core.domain.model.VideoFormatOption
import com.socialvideodownloader.core.domain.model.VideoMetadata
import com.socialvideodownloader.core.domain.usecase.ExtractVideoInfoUseCase
import com.socialvideodownloader.core.domain.usecase.GetClipboardUrlUseCase
import com.socialvideodownloader.feature.download.service.DownloadServiceState
import com.socialvideodownloader.feature.download.service.DownloadServiceStateHolder
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
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
    private lateinit var getClipboardUrl: GetClipboardUrlUseCase
    private lateinit var errorMessageMapper: ErrorMessageMapper
    private lateinit var serviceStateHolder: DownloadServiceStateHolder
    private lateinit var context: Context
    private lateinit var viewModel: DownloadViewModel

    private val testMetadata = VideoMetadata(
        sourceUrl = "https://youtube.com/watch?v=test",
        title = "Test Video",
        thumbnailUrl = "https://thumb.jpg",
        durationSeconds = 120,
        author = "Author",
        formats = listOf(
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
        getClipboardUrl = mockk()
        errorMessageMapper = mockk()
        serviceStateHolder = DownloadServiceStateHolder()
        context = mockk(relaxed = true)
        every { getClipboardUrl() } returns null
        every { errorMessageMapper.map(any()) } answers { firstArg<Throwable>().message ?: "Error" }
        viewModel = DownloadViewModel(
            extractVideoInfo = extractVideoInfo,
            getClipboardUrl = getClipboardUrl,
            errorMessageMapper = errorMessageMapper,
            serviceStateHolder = serviceStateHolder,
            context = context,
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Idle`() = runTest {
        viewModel.uiState.test {
            assertTrue(awaitItem() is DownloadUiState.Idle)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `ExtractClicked transitions to Extracting then FormatSelection on success`() = runTest {
        coEvery { extractVideoInfo(any()) } returns Result.success(testMetadata)

        viewModel.uiState.test {
            assertTrue(awaitItem() is DownloadUiState.Idle)

            viewModel.onIntent(DownloadIntent.UrlChanged("https://youtube.com/watch?v=test"))
            viewModel.onIntent(DownloadIntent.ExtractClicked)

            val extracting = awaitItem()
            assertTrue(extracting is DownloadUiState.Extracting)

            val formatSelection = awaitItem()
            assertTrue(formatSelection is DownloadUiState.FormatSelection)
            assertEquals("248", (formatSelection as DownloadUiState.FormatSelection).selectedFormatId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `ExtractClicked transitions to Error on failure`() = runTest {
        coEvery { extractVideoInfo(any()) } returns Result.failure(RuntimeException("Network error"))

        viewModel.uiState.test {
            assertTrue(awaitItem() is DownloadUiState.Idle)

            viewModel.onIntent(DownloadIntent.UrlChanged("https://youtube.com/watch?v=test"))
            viewModel.onIntent(DownloadIntent.ExtractClicked)

            assertTrue(awaitItem() is DownloadUiState.Extracting)

            val error = awaitItem()
            assertTrue(error is DownloadUiState.Error)
            assertEquals("Network error", (error as DownloadUiState.Error).message)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `FormatSelected updates selectedFormatId`() = runTest {
        coEvery { extractVideoInfo(any()) } returns Result.success(testMetadata)

        viewModel.uiState.test {
            skipItems(1) // Idle
            viewModel.onIntent(DownloadIntent.UrlChanged("url"))
            viewModel.onIntent(DownloadIntent.ExtractClicked)
            skipItems(1) // Extracting

            val initial = awaitItem() as DownloadUiState.FormatSelection
            assertEquals("248", initial.selectedFormatId)

            viewModel.onIntent(DownloadIntent.FormatSelected("136"))

            val updated = awaitItem() as DownloadUiState.FormatSelection
            assertEquals("136", updated.selectedFormatId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `best format is pre-selected as first non-audio format`() = runTest {
        val metadataWithAudioFirst = testMetadata.copy(
            formats = listOf(
                VideoFormatOption("251", "opus", null, "webm", 5_000_000L, isAudioOnly = true, isVideoOnly = false),
                VideoFormatOption("248", "1080p", 1080, "mp4", 100_000_000L, isAudioOnly = false, isVideoOnly = false),
            ),
        )
        coEvery { extractVideoInfo(any()) } returns Result.success(metadataWithAudioFirst)

        viewModel.uiState.test {
            skipItems(1) // Idle
            viewModel.onIntent(DownloadIntent.UrlChanged("url"))
            viewModel.onIntent(DownloadIntent.ExtractClicked)
            skipItems(1) // Extracting

            val selection = awaitItem() as DownloadUiState.FormatSelection
            assertEquals("248", selection.selectedFormatId)
            assertFalse(selection.metadata.formats.first { it.formatId == "248" }.isAudioOnly)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `ExtractClicked with blank url does not transition state`() = runTest {
        viewModel.uiState.test {
            assertTrue(awaitItem() is DownloadUiState.Idle)
            viewModel.onIntent(DownloadIntent.ExtractClicked)
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Error then RetryClicked transitions to Extracting`() = runTest {
        coEvery { extractVideoInfo(any()) } returnsMany listOf(
            Result.failure(RuntimeException("Network error")),
            Result.success(testMetadata),
        )

        viewModel.onIntent(DownloadIntent.UrlChanged("https://youtube.com/watch?v=test"))
        viewModel.onIntent(DownloadIntent.ExtractClicked)
        advanceUntilIdle()

        viewModel.uiState.test {
            val current = awaitItem()
            assertTrue(current is DownloadUiState.Error)

            viewModel.onIntent(DownloadIntent.RetryClicked)

            val extracting = awaitItem()
            assertTrue(extracting is DownloadUiState.Extracting)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Error then NewDownloadClicked transitions to Idle`() = runTest {
        coEvery { extractVideoInfo(any()) } returns Result.failure(RuntimeException("Network error"))

        viewModel.onIntent(DownloadIntent.UrlChanged("https://youtube.com/watch?v=test"))
        viewModel.onIntent(DownloadIntent.ExtractClicked)
        advanceUntilIdle()

        viewModel.uiState.test {
            val current = awaitItem()
            assertTrue(current is DownloadUiState.Error)

            viewModel.onIntent(DownloadIntent.NewDownloadClicked)

            val idle = awaitItem()
            assertTrue(idle is DownloadUiState.Idle)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `ClipboardUrlDetected in Idle state sets clipboardUrl`() = runTest {
        every { getClipboardUrl() } returns "https://youtube.com/watch?v=clipboard"
        val freshViewModel = DownloadViewModel(
            extractVideoInfo = extractVideoInfo,
            getClipboardUrl = getClipboardUrl,
            errorMessageMapper = errorMessageMapper,
            serviceStateHolder = serviceStateHolder,
            context = context,
        )

        freshViewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is DownloadUiState.Idle)
            assertEquals(
                "https://youtube.com/watch?v=clipboard",
                (state as DownloadUiState.Idle).clipboardUrl,
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `DownloadClicked transitions to Downloading state with correct selectedFormatId`() = runTest {
        coEvery { extractVideoInfo(any()) } returns Result.success(testMetadata)

        viewModel.onIntent(DownloadIntent.UrlChanged("https://youtube.com/watch?v=test"))
        viewModel.onIntent(DownloadIntent.ExtractClicked)
        advanceUntilIdle()

        viewModel.uiState.test {
            val formatSelection = awaitItem() as DownloadUiState.FormatSelection
            assertEquals("248", formatSelection.selectedFormatId)

            viewModel.onIntent(DownloadIntent.DownloadClicked)

            val downloading = awaitItem() as DownloadUiState.Downloading
            assertEquals("248", downloading.selectedFormatId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `service state Completed transitions to Done with correct filePath`() = runTest {
        coEvery { extractVideoInfo(any()) } returns Result.success(testMetadata)

        viewModel.onIntent(DownloadIntent.UrlChanged("https://youtube.com/watch?v=test"))
        viewModel.onIntent(DownloadIntent.ExtractClicked)
        advanceUntilIdle()
        viewModel.onIntent(DownloadIntent.DownloadClicked)
        advanceUntilIdle()

        viewModel.uiState.test {
            val downloading = awaitItem() as DownloadUiState.Downloading
            val requestId = downloading.progress.requestId

            serviceStateHolder.update(DownloadServiceState.Completed(requestId, "/path/to/file"))

            val done = awaitItem() as DownloadUiState.Done
            assertEquals("/path/to/file", done.filePath)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `service state Failed transitions to Error with mapped message`() = runTest {
        coEvery { extractVideoInfo(any()) } returns Result.success(testMetadata)
        every { errorMessageMapper.map(any()) } returns "Download error"

        viewModel.onIntent(DownloadIntent.UrlChanged("https://youtube.com/watch?v=test"))
        viewModel.onIntent(DownloadIntent.ExtractClicked)
        advanceUntilIdle()
        viewModel.onIntent(DownloadIntent.DownloadClicked)
        advanceUntilIdle()

        viewModel.uiState.test {
            val downloading = awaitItem() as DownloadUiState.Downloading
            val requestId = downloading.progress.requestId

            serviceStateHolder.update(DownloadServiceState.Failed(requestId, "Download error"))

            val error = awaitItem() as DownloadUiState.Error
            assertEquals("Download error", error.message)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `service state Cancelled restores FormatSelection with original formatId`() = runTest {
        coEvery { extractVideoInfo(any()) } returns Result.success(testMetadata)

        viewModel.onIntent(DownloadIntent.UrlChanged("https://youtube.com/watch?v=test"))
        viewModel.onIntent(DownloadIntent.ExtractClicked)
        advanceUntilIdle()
        viewModel.onIntent(DownloadIntent.FormatSelected("136"))
        viewModel.onIntent(DownloadIntent.DownloadClicked)
        advanceUntilIdle()

        viewModel.uiState.test {
            val downloading = awaitItem() as DownloadUiState.Downloading
            val requestId = downloading.progress.requestId
            assertEquals("136", downloading.selectedFormatId)

            serviceStateHolder.update(DownloadServiceState.Cancelled(requestId))

            val formatSelection = awaitItem() as DownloadUiState.FormatSelection
            assertEquals("136", formatSelection.selectedFormatId)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
