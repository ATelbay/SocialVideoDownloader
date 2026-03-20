package com.socialvideodownloader.feature.download.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.socialvideodownloader.core.domain.model.DownloadProgress
import com.socialvideodownloader.core.domain.model.DownloadRequest
import com.socialvideodownloader.core.domain.usecase.ExtractVideoInfoUseCase
import com.socialvideodownloader.core.domain.usecase.FindExistingDownloadUseCase
import com.socialvideodownloader.feature.download.R
import com.socialvideodownloader.feature.download.service.DownloadService
import com.socialvideodownloader.feature.download.service.DownloadServiceState
import com.socialvideodownloader.feature.download.service.DownloadServiceStateHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
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
    private val findExistingDownload: FindExistingDownloadUseCase,
    private val errorMessageMapper: ErrorMessageMapper,
    private val serviceStateHolder: DownloadServiceStateHolder,
    @ApplicationContext private val context: Context,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow<DownloadUiState>(DownloadUiState.Idle())
    val uiState: StateFlow<DownloadUiState> = _uiState.asStateFlow()

    private val _events = Channel<DownloadEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var currentUrl: String = ""
    private var duplicateCheckJob: Job? = null
    private var pendingShareOnly: Boolean = false

    init {
        collectServiceState()
        val initialUrl: String? = savedStateHandle["initialUrl"]
        val savedUrl: String? = savedStateHandle["currentUrl"]
        val url = initialUrl ?: savedUrl
        if (url != null) {
            currentUrl = url
            _uiState.value = DownloadUiState.Idle()
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
                        if (downloading.isShareMode) {
                            _events.send(DownloadEvent.ShareFile(serviceState.filePath))
                            _uiState.value = DownloadUiState.FormatSelection(
                                metadata = downloading.metadata,
                                selectedFormatId = downloading.selectedFormatId,
                            )
                        } else {
                            _uiState.value = DownloadUiState.Done(
                                metadata = downloading.metadata,
                                filePath = serviceState.filePath,
                            )
                        }
                    }
                    is DownloadServiceState.Failed -> {
                        val downloading = current as? DownloadUiState.Downloading ?: return@collect
                        if (downloading.progress.requestId != serviceState.requestId) return@collect
                        if (downloading.isShareMode) {
                            _events.send(DownloadEvent.ShowSnackbar(errorMessageMapper.map(Exception(serviceState.error))))
                            _uiState.value = DownloadUiState.FormatSelection(
                                metadata = downloading.metadata,
                                selectedFormatId = downloading.selectedFormatId,
                            )
                        } else {
                            _uiState.value = DownloadUiState.Error(
                                message = errorMessageMapper.map(Exception(serviceState.error)),
                                retryAction = RetryAction.RetryExtraction(currentUrl),
                            )
                        }
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
                    is DownloadServiceState.Idle -> Unit
                    is DownloadServiceState.Queued -> {
                        _events.send(DownloadEvent.ShowSnackbar(context.getString(R.string.download_queued)))
                    }
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
            is DownloadIntent.OpenExistingClicked -> handleOpenExisting()
            is DownloadIntent.ShareExistingClicked -> handleShareExisting()
            is DownloadIntent.ShareFormatClicked -> handleShareFormat()
            is DownloadIntent.DismissExistingBanner -> handleDismissExistingBanner()
        }
    }

    private fun handleUrlChanged(url: String) {
        currentUrl = url
        savedStateHandle["currentUrl"] = url

        duplicateCheckJob?.cancel()
        if (url.isBlank()) {
            val current = _uiState.value
            if (current is DownloadUiState.Idle && current.existingDownload != null) {
                _uiState.value = DownloadUiState.Idle()
            }
            return
        }
        duplicateCheckJob = viewModelScope.launch {
            delay(500)
            val existing = findExistingDownload(url)
            val current = _uiState.value
            if (current is DownloadUiState.Idle) {
                _uiState.value = DownloadUiState.Idle(existingDownload = existing)
            }
        }
    }

    private fun handleExtract() {
        if (currentUrl.isBlank()) return
        duplicateCheckJob?.cancel()
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
        startDownloadWithPermissionCheck(shareOnly = false)
    }

    fun onNotificationPermissionResult(granted: Boolean) {
        if (!granted) {
            viewModelScope.launch {
                _events.send(DownloadEvent.ShowSnackbar(context.getString(R.string.notification_permission_rationale)))
            }
        }
        // Proceed with download regardless of permission result
        startDownload(pendingShareOnly)
    }

    private fun handleShareFormat() {
        startDownloadWithPermissionCheck(shareOnly = true)
    }

    private fun startDownloadWithPermissionCheck(shareOnly: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                pendingShareOnly = shareOnly
                viewModelScope.launch { _events.send(DownloadEvent.RequestNotificationPermission) }
                return
            }
        }
        startDownload(shareOnly)
    }

    private fun startDownload(shareOnly: Boolean = false) {
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
            totalBytes = selectedFormat.fileSizeBytes,
            shareOnly = shareOnly,
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
            isShareMode = shareOnly,
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
            putExtra(DownloadService.EXTRA_SHARE_ONLY, request.shareOnly)
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
        when (val action = state.retryAction) {
            is RetryAction.RetryExtraction -> {
                currentUrl = action.url
                handleExtract()
            }
        }
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
        duplicateCheckJob?.cancel()
        _uiState.value = DownloadUiState.Idle()
    }

    private fun handleOpenExisting() {
        val state = _uiState.value
        if (state is DownloadUiState.Idle) {
            val existing = state.existingDownload ?: return
            viewModelScope.launch {
                _events.send(DownloadEvent.OpenFile(existing.contentUri))
            }
        }
    }

    private fun handleShareExisting() {
        val state = _uiState.value
        if (state is DownloadUiState.Idle) {
            val existing = state.existingDownload ?: return
            viewModelScope.launch {
                _events.send(DownloadEvent.ShareFile(existing.contentUri))
            }
        }
    }

    private fun handleDismissExistingBanner() {
        val current = _uiState.value
        if (current is DownloadUiState.Idle) {
            _uiState.value = DownloadUiState.Idle(prefillUrl = current.prefillUrl)
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Only sweep leftover share temp files when the service is not active.
        // Per-file cleanup runs eagerly after each share chooser launches;
        // this is a backstop for process kills or edge cases.
        // Note: viewModelScope is cancelled by the time onCleared runs, so use a
        // standalone scope for fire-and-forget IO work.
        if (serviceStateHolder.state.value is DownloadServiceState.Idle) {
            CoroutineScope(Dispatchers.IO).launch {
                java.io.File(context.cacheDir, DownloadService.SHARE_TEMP_DIR).deleteRecursively()
            }
        }
    }

    private fun handlePrefillUrl(url: String) {
        duplicateCheckJob?.cancel()
        currentUrl = url
        viewModelScope.launch {
            val existing = findExistingDownload(url)
            if (existing != null) {
                _uiState.value = DownloadUiState.Idle(
                    existingDownload = existing,
                    prefillUrl = url,
                )
            } else {
                handleExtract()
            }
        }
    }
}

sealed interface DownloadEvent {
    data class OpenFile(val filePath: String) : DownloadEvent
    data class ShareFile(val filePath: String) : DownloadEvent
    data class ShowSnackbar(val message: String) : DownloadEvent
    data object RequestNotificationPermission : DownloadEvent
}
