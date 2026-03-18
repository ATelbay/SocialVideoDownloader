package com.socialvideodownloader.feature.download.ui

import android.content.Context
import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.socialvideodownloader.core.domain.model.DownloadProgress
import com.socialvideodownloader.core.domain.model.DownloadRequest
import com.socialvideodownloader.core.domain.usecase.ExtractVideoInfoUseCase
import com.socialvideodownloader.feature.download.service.DownloadService
import com.socialvideodownloader.feature.download.service.DownloadServiceState
import com.socialvideodownloader.feature.download.service.DownloadServiceStateHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class DownloadViewModel @Inject constructor(
    private val extractVideoInfo: ExtractVideoInfoUseCase,
    private val errorMessageMapper: ErrorMessageMapper,
    private val serviceStateHolder: DownloadServiceStateHolder,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow<DownloadUiState>(DownloadUiState.Idle)
    val uiState: StateFlow<DownloadUiState> = _uiState.asStateFlow()

    private val _events = Channel<DownloadEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var currentUrl: String = ""

    init {
        collectServiceState()
        val initialUrl: String? = savedStateHandle["initialUrl"]
        if (initialUrl != null) {
            currentUrl = initialUrl
            _uiState.value = DownloadUiState.Idle
        }
    }

    private fun collectServiceState() {
        viewModelScope.launch {
            serviceStateHolder.state.collect { serviceState ->
                val current = _uiState.value
                when (serviceState) {
                    is DownloadServiceState.Downloading -> {
                        if (current is DownloadUiState.Downloading) {
                            _uiState.value = current.copy(progress = serviceState.progress)
                        } else if (current is DownloadUiState.FormatSelection) {
                            _uiState.value = DownloadUiState.Downloading(
                                metadata = current.metadata,
                                progress = serviceState.progress,
                                selectedFormatId = current.selectedFormatId,
                            )
                        }
                    }
                    is DownloadServiceState.Completed -> {
                        val downloading = current as? DownloadUiState.Downloading ?: return@collect
                        if (downloading.progress.requestId != serviceState.requestId) return@collect
                        _uiState.value = DownloadUiState.Done(
                            metadata = downloading.metadata,
                            filePath = serviceState.filePath,
                        )
                    }
                    is DownloadServiceState.Failed -> {
                        val downloading = current as? DownloadUiState.Downloading ?: return@collect
                        if (downloading.progress.requestId != serviceState.requestId) return@collect
                        _uiState.value = DownloadUiState.Error(
                            message = errorMessageMapper.map(Exception(serviceState.error)),
                            retryAction = RetryAction.RetryExtraction(currentUrl),
                        )
                    }
                    is DownloadServiceState.Cancelled -> {
                        if (current is DownloadUiState.Downloading &&
                            current.progress.requestId == serviceState.requestId
                        ) {
                            _uiState.value = DownloadUiState.FormatSelection(
                                metadata = current.metadata,
                                selectedFormatId = current.selectedFormatId,
                            )
                        }
                    }
                    is DownloadServiceState.Idle,
                    is DownloadServiceState.Queued,
                    -> Unit
                }
            }
        }
    }

    fun onIntent(intent: DownloadIntent) {
        when (intent) {
            is DownloadIntent.UrlChanged -> handleUrlChanged(intent.url)
            is DownloadIntent.ExtractClicked -> handleExtract()
            is DownloadIntent.FormatSelected -> handleFormatSelected(intent.formatId)
            is DownloadIntent.DownloadClicked -> handleDownload()
            is DownloadIntent.CancelDownloadClicked -> handleCancel()
            is DownloadIntent.RetryClicked -> handleRetry()
            is DownloadIntent.OpenFileClicked -> handleOpenFile()
            is DownloadIntent.ShareFileClicked -> handleShareFile()
            is DownloadIntent.NewDownloadClicked -> handleNewDownload()
            is DownloadIntent.PrefillUrl -> handlePrefillUrl(intent.url)
        }
    }

    private fun handleUrlChanged(url: String) {
        currentUrl = url
    }

    private fun handleExtract() {
        if (currentUrl.isBlank()) return
        _uiState.value = DownloadUiState.Extracting(currentUrl)

        viewModelScope.launch {
            extractVideoInfo(currentUrl)
                .onSuccess { metadata ->
                    val bestFormatId = metadata.formats
                        .firstOrNull { !it.isAudioOnly }?.formatId
                        ?: metadata.formats.firstOrNull()?.formatId
                        ?: ""
                    _uiState.value = DownloadUiState.FormatSelection(
                        metadata = metadata,
                        selectedFormatId = bestFormatId,
                    )
                }
                .onFailure { error ->
                    _uiState.value = DownloadUiState.Error(
                        message = errorMessageMapper.map(error),
                        retryAction = RetryAction.RetryExtraction(currentUrl),
                    )
                }
        }
    }

    private fun handleFormatSelected(formatId: String) {
        val state = _uiState.value
        if (state is DownloadUiState.FormatSelection) {
            _uiState.value = state.copy(selectedFormatId = formatId)
        }
    }

    private fun handleDownload() {
        val state = _uiState.value
        if (state !is DownloadUiState.FormatSelection) return

        val selectedFormat = state.metadata.formats
            .find { it.formatId == state.selectedFormatId } ?: return
        val requestId = UUID.randomUUID().toString()

        val request = DownloadRequest(
            id = requestId,
            sourceUrl = state.metadata.sourceUrl,
            videoTitle = state.metadata.title,
            thumbnailUrl = state.metadata.thumbnailUrl,
            formatId = selectedFormat.formatId,
            formatLabel = selectedFormat.label,
            isVideoOnly = selectedFormat.isVideoOnly,
        )

        _uiState.value = DownloadUiState.Downloading(
            metadata = state.metadata,
            progress = DownloadProgress(
                requestId = requestId,
                progressPercent = 0f,
                downloadedBytes = 0L,
                totalBytes = selectedFormat.fileSizeBytes,
                speedBytesPerSec = 0L,
                etaSeconds = 0L,
            ),
            selectedFormatId = selectedFormat.formatId,
        )

        val serviceIntent = Intent(context, DownloadService::class.java).apply {
            action = DownloadService.ACTION_START_DOWNLOAD
            putExtra(DownloadService.EXTRA_REQUEST_ID, request.id)
            putExtra(DownloadService.EXTRA_SOURCE_URL, request.sourceUrl)
            putExtra(DownloadService.EXTRA_VIDEO_TITLE, request.videoTitle)
            putExtra(DownloadService.EXTRA_THUMBNAIL_URL, request.thumbnailUrl)
            putExtra(DownloadService.EXTRA_FORMAT_ID, request.formatId)
            putExtra(DownloadService.EXTRA_FORMAT_LABEL, request.formatLabel)
            putExtra(DownloadService.EXTRA_IS_VIDEO_ONLY, request.isVideoOnly)
        }
        context.startForegroundService(serviceIntent)
    }

    private fun handleCancel() {
        val state = _uiState.value
        val requestId = (state as? DownloadUiState.Downloading)?.progress?.requestId ?: return
        val serviceIntent = Intent(context, DownloadService::class.java).apply {
            action = DownloadService.ACTION_CANCEL_DOWNLOAD
            putExtra(DownloadService.EXTRA_REQUEST_ID, requestId)
        }
        context.startService(serviceIntent)
    }

    private fun handleRetry() {
        val state = _uiState.value
        if (state !is DownloadUiState.Error) return
        val action = state.retryAction as RetryAction.RetryExtraction
        currentUrl = action.url
        handleExtract()
    }

    private fun handleOpenFile() {
        val state = _uiState.value
        if (state is DownloadUiState.Done) {
            viewModelScope.launch {
                _events.send(DownloadEvent.OpenFile(state.filePath))
            }
        }
    }

    private fun handleShareFile() {
        val state = _uiState.value
        if (state is DownloadUiState.Done) {
            viewModelScope.launch {
                _events.send(DownloadEvent.ShareFile(state.filePath))
            }
        }
    }

    private fun handleNewDownload() {
        currentUrl = ""
        _uiState.value = DownloadUiState.Idle
    }

    private fun handlePrefillUrl(url: String) {
        currentUrl = url
        handleExtract()
    }
}

sealed interface DownloadEvent {
    data class OpenFile(val filePath: String) : DownloadEvent
    data class ShareFile(val filePath: String) : DownloadEvent
    data class ShowSnackbar(val message: String) : DownloadEvent
    data object RequestNotificationPermission : DownloadEvent
}
