package com.socialvideodownloader.shared.feature.download

import com.socialvideodownloader.core.domain.model.DownloadProgress
import com.socialvideodownloader.core.domain.model.DownloadRequest
import com.socialvideodownloader.core.domain.usecase.ExtractVideoInfoUseCase
import com.socialvideodownloader.core.domain.usecase.FindExistingDownloadUseCase
import com.socialvideodownloader.shared.data.platform.DownloadErrorType
import com.socialvideodownloader.shared.data.platform.DownloadServiceState
import com.socialvideodownloader.shared.data.platform.PlatformDownloadManager
import com.socialvideodownloader.shared.network.auth.CookieStore
import com.socialvideodownloader.shared.network.auth.SupportedPlatform
import com.socialvideodownloader.shared.network.auth.detectPlatform
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * Platform-independent shared ViewModel for the download screen.
 *
 * Contains all state machine logic extracted from the Android DownloadViewModel.
 * Platform-specific concerns (notification permissions, service binding) are
 * delegated back to the Android ViewModel via [PlatformDelegate].
 *
 * Threading: [coroutineScope] is expected to be main-confined — on Android it is
 * `viewModelScope` (Main.immediate), on iOS the SwiftUI-owned main scope. All
 * mutable bookkeeping fields ([currentUrl], [backgroundDownload], [pendingShareOnly],
 * [existingRecordId], the cached jobs) are therefore only touched from the main thread
 * and need no extra synchronization. Do not pass a multi-threaded dispatcher here.
 */
class SharedDownloadViewModel(
    private val coroutineScope: CoroutineScope,
    private val extractVideoInfo: ExtractVideoInfoUseCase,
    private val findExistingDownload: FindExistingDownloadUseCase,
    private val platformDownloadManager: PlatformDownloadManager,
    private val secureCookieStore: CookieStore,
    private val initialUrl: String? = null,
    private val savedUrl: String? = null,
) {
    /**
     * Callback interface for platform-specific actions that the shared VM
     * needs to trigger but cannot perform itself (e.g., notification permission
     * on Android, share sheet on iOS).
     */
    interface PlatformDelegate {
        /** Called when the VM wants to check/request notification permission before starting a download. */
        fun checkNotificationPermission(pendingShareOnly: Boolean)

        fun showPlatformLogin(platform: SupportedPlatform)
    }

    var platformDelegate: PlatformDelegate? = null

    /** Maximum number of automatic retry attempts for transient (network/server) errors. */
    private val maxRetryAttempts = 3

    /** Base delay in milliseconds for exponential backoff between retries. */
    private val retryBaseDelayMs = 1_000L

    private val _uiState =
        MutableStateFlow<DownloadUiState>(DownloadUiState.Idle(connectedPlatforms = secureCookieStore.connectedPlatforms()))
    val uiState: StateFlow<DownloadUiState> = _uiState.asStateFlow()

    private val _events = Channel<DownloadEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var currentUrl: String = ""
    private var backgroundDownload: DownloadUiState.Downloading? = null
    private var duplicateCheckJob: Job? = null
    private var extractionJob: Job? = null
    private var pendingShareOnly: Boolean = false
    private var existingRecordId: Long? = null

    init {
        collectServiceState()
        val url = initialUrl ?: savedUrl
        if (url != null) {
            currentUrl = url
            _uiState.value =
                DownloadUiState.Idle(
                    prefillUrl = url,
                    connectedPlatforms = secureCookieStore.connectedPlatforms(),
                )
        }
    }

    private fun collectServiceState() {
        coroutineScope.launch {
            platformDownloadManager.downloadState.collect { serviceState ->
                val current = _uiState.value
                when (serviceState) {
                    is DownloadServiceState.Downloading -> {
                        // Only update progress for the download we are actively showing.
                        // startDownload() already creates the Downloading state synchronously,
                        // so we never reconstruct it here (which would lose isShareMode).
                        if (current is DownloadUiState.Downloading &&
                            current.progress.requestId == serviceState.requestId
                        ) {
                            _uiState.value = current.copy(progress = serviceState.progress)
                        }
                    }
                    is DownloadServiceState.Completed -> {
                        val foreground =
                            (current as? DownloadUiState.Downloading)
                                ?.takeIf { it.progress.requestId == serviceState.requestId }
                        if (foreground != null) {
                            if (foreground.isShareMode) {
                                _events.send(DownloadEvent.ShareFile(serviceState.fileUri ?: serviceState.filePath))
                                _uiState.value =
                                    DownloadUiState.FormatSelection(
                                        metadata = foreground.metadata,
                                        selectedFormatId = foreground.selectedFormatId,
                                    )
                            } else {
                                _uiState.value =
                                    DownloadUiState.Done(
                                        metadata = foreground.metadata,
                                        filePath = serviceState.filePath,
                                        fileUri = serviceState.fileUri,
                                    )
                            }
                            return@collect
                        }
                        // Terminal event for a backgrounded download (user moved on via
                        // New Download). Never clobber an active new flow: only surface the
                        // result when the user is idle; for share-mode just emit the event.
                        val bg = backgroundDownload?.takeIf { it.progress.requestId == serviceState.requestId }
                        if (bg != null) {
                            backgroundDownload = null
                            if (bg.isShareMode) {
                                _events.send(DownloadEvent.ShareFile(serviceState.fileUri ?: serviceState.filePath))
                            } else if (current is DownloadUiState.Idle) {
                                _uiState.value =
                                    DownloadUiState.Done(
                                        metadata = bg.metadata,
                                        filePath = serviceState.filePath,
                                        fileUri = serviceState.fileUri,
                                    )
                            }
                        }
                    }
                    is DownloadServiceState.Failed -> {
                        val foreground =
                            (current as? DownloadUiState.Downloading)
                                ?.takeIf { it.progress.requestId == serviceState.requestId }
                        if (foreground != null) {
                            if (foreground.isShareMode) {
                                _events.send(DownloadEvent.ShowError(serviceState.error, message = null))
                                _uiState.value =
                                    DownloadUiState.FormatSelection(
                                        metadata = foreground.metadata,
                                        selectedFormatId = foreground.selectedFormatId,
                                    )
                            } else {
                                _uiState.value =
                                    DownloadUiState.Error(
                                        errorType = serviceState.error,
                                        message = null,
                                        // Retry the request's own source URL, not the possibly-changed currentUrl.
                                        retryAction = RetryAction.RetryExtraction(foreground.metadata.sourceUrl),
                                    )
                            }
                            return@collect
                        }
                        val bg = backgroundDownload?.takeIf { it.progress.requestId == serviceState.requestId }
                        if (bg != null) {
                            backgroundDownload = null
                            if (bg.isShareMode) {
                                _events.send(DownloadEvent.ShowError(serviceState.error, message = null))
                            } else if (current is DownloadUiState.Idle) {
                                _uiState.value =
                                    DownloadUiState.Error(
                                        errorType = serviceState.error,
                                        message = null,
                                        retryAction = RetryAction.RetryExtraction(bg.metadata.sourceUrl),
                                    )
                            } else {
                                // User is mid-another-flow: don't clobber it with an Error state,
                                // but still surface the failure so a backgrounded download never
                                // fails silently.
                                _events.send(DownloadEvent.ShowError(serviceState.error, message = null))
                            }
                        }
                    }
                    is DownloadServiceState.Cancelled -> {
                        if (current is DownloadUiState.Downloading &&
                            current.progress.requestId == serviceState.requestId
                        ) {
                            _uiState.value =
                                DownloadUiState.FormatSelection(
                                    metadata = current.metadata,
                                    selectedFormatId = current.selectedFormatId,
                                )
                        } else {
                            backgroundDownload?.takeIf { it.progress.requestId == serviceState.requestId }
                                ?.let { backgroundDownload = null }
                        }
                    }
                    is DownloadServiceState.Idle -> {
                        // Drop a stale backgrounded download once the manager no longer tracks
                        // it (e.g. its process died without emitting a terminal event). Guarded
                        // by activeRequestId so an actively-running background download is never
                        // discarded; this only frees a dead reference.
                        val bg = backgroundDownload
                        if (bg != null && bg.progress.requestId != platformDownloadManager.activeRequestId) {
                            backgroundDownload = null
                        }
                    }
                    is DownloadServiceState.Queued -> {
                        // Platform layer may show its own toast/snackbar for "queued"
                        // No shared state change needed
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
            is DownloadIntent.PrefillUrl -> handlePrefillUrl(intent.url, intent.existingRecordId)
            is DownloadIntent.OpenExistingClicked -> handleOpenExisting()
            is DownloadIntent.ShareExistingClicked -> handleShareExisting()
            is DownloadIntent.ShareFormatClicked -> handleShareFormat()
            is DownloadIntent.DismissExistingBanner -> handleDismissExistingBanner()
            is DownloadIntent.BackToIdleClicked -> handleBackToIdle()
            is DownloadIntent.ConnectPlatformClicked -> handleConnectPlatform(intent.platform)
            is DownloadIntent.PlatformLoginResult -> handlePlatformLoginResult(intent.platform, intent.success)
            is DownloadIntent.DisconnectPlatformClicked -> handleDisconnectPlatform(intent.platform)
        }
    }

    private fun handleUrlChanged(url: String) {
        currentUrl = url
        existingRecordId = null

        duplicateCheckJob?.cancel()
        if (url.isBlank()) {
            val current = _uiState.value
            if (current is DownloadUiState.Idle && current.existingDownload != null) {
                _uiState.value = DownloadUiState.Idle(connectedPlatforms = secureCookieStore.connectedPlatforms())
            }
            return
        }
        duplicateCheckJob =
            coroutineScope.launch {
                delay(500)
                val existing = findExistingDownload(url)
                val current = _uiState.value
                if (current is DownloadUiState.Idle) {
                    // copy() so an active prefillUrl (e.g. from a share intent) survives the
                    // duplicate-found update instead of being dropped.
                    _uiState.value =
                        current.copy(
                            existingDownload = existing,
                            connectedPlatforms = secureCookieStore.connectedPlatforms(),
                        )
                }
            }
    }

    private fun handleExtract() {
        if (currentUrl.isBlank()) return
        duplicateCheckJob?.cancel()
        extractionJob?.cancel()
        val url = currentUrl
        _uiState.value = DownloadUiState.Extracting(url)

        extractionJob =
            coroutineScope.launch {
                extractWithRetry(url)
            }
    }

    /**
     * Attempts to extract video info with automatic retry for transient errors.
     *
     * Retries up to [maxRetryAttempts] times with exponential backoff when the
     * error is network-related (server unavailable, connection refused, timeout).
     * Permanent errors (unsupported URL, private video) are not retried.
     */
    private suspend fun extractWithRetry(url: String) {
        var attempt = 0
        while (true) {
            val result = extractVideoInfo(url)
            result
                .onSuccess { metadata ->
                    // Guard against a superseded extraction (a newer URL was started via
                    // prefill / login-retry while this one was in flight). Cancellation alone
                    // can't cover this: there is no suspension point between the network return
                    // and this state write, so a cancelled continuation would still run.
                    if (!isCurrentExtraction(url)) return
                    // Prefer the first video format (formats are expected pre-sorted
                    // best-first by the extractor); fall back to any audio-only format.
                    val bestFormatId =
                        metadata.formats.firstOrNull { !it.isAudioOnly }?.formatId
                            ?: metadata.formats.firstOrNull()?.formatId
                    if (bestFormatId == null) {
                        // Extraction succeeded but yielded no usable formats — treat as a failure
                        // instead of dropping the user into a dead-end empty FormatSelection.
                        _uiState.value =
                            DownloadUiState.Error(
                                errorType = DownloadErrorType.EXTRACTION_FAILED,
                                message = null,
                                retryAction = RetryAction.RetryExtraction(url),
                            )
                        return
                    }
                    _uiState.value =
                        DownloadUiState.FormatSelection(
                            metadata = metadata,
                            selectedFormatId = bestFormatId,
                        )
                    return
                }
                .onFailure { error ->
                    if (error is CancellationException) throw error

                    val errorType = DownloadErrorClassifier.classify(error, url)
                    val isTransient = isTransientError(errorType)

                    if (isTransient && attempt < maxRetryAttempts) {
                        attempt++
                        val backoffMs = retryBaseDelayMs * (1L shl (attempt - 1)) // 1s, 2s, 4s
                        delay(backoffMs)
                        // Only retry if still extracting THIS url (user hasn't navigated away
                        // or started a different extraction).
                        if (!isCurrentExtraction(url)) return
                    } else {
                        if (!isCurrentExtraction(url)) return
                        val platform = if (errorType == DownloadErrorType.AUTH_REQUIRED) detectPlatform(url) else null
                        // If cookies exist but extraction still failed, show "Reconnect" label
                        // but DON'T clear cookies — they may still be valid for retry.
                        // Cookies are only replaced when the user completes a new login.
                        val isReconnect = platform != null && secureCookieStore.isConnected(platform)
                        // The UI resolves user-facing text from [errorType] via string resources.
                        // Only pass the raw error through for UNKNOWN as a last-resort technical
                        // detail (an arbitrary extractor error is inherently un-localizable).
                        val rawDetail = if (errorType == DownloadErrorType.UNKNOWN) error.message else null
                        _uiState.value =
                            DownloadUiState.Error(
                                errorType = errorType,
                                message = rawDetail,
                                retryAction = RetryAction.RetryExtraction(url),
                                platformForAuth = platform,
                                isReconnect = isReconnect,
                            )
                        return
                    }
                }
        }
    }

    /**
     * True while the VM is still showing [DownloadUiState.Extracting] for [url].
     * Extraction results (success, no-format, terminal error) are applied only when
     * current, so a superseded extraction can never clobber the live state.
     */
    private fun isCurrentExtraction(url: String): Boolean {
        val state = _uiState.value
        return state is DownloadUiState.Extracting && state.url == url
    }

    /**
     * Returns true for errors that are transient and worth retrying automatically:
     * network errors and server unavailability. Permanent errors (unsupported URL,
     * private/deleted video, storage issues) return false.
     */
    private fun isTransientError(errorType: DownloadErrorType): Boolean =
        errorType == DownloadErrorType.NETWORK_ERROR ||
            errorType == DownloadErrorType.SERVER_UNAVAILABLE

    private fun handleFormatSelected(formatId: String) {
        val state = _uiState.value
        if (state is DownloadUiState.FormatSelection) {
            _uiState.value = state.copy(selectedFormatId = formatId)
        }
    }

    private fun handleDownload() {
        startDownloadWithPermissionCheck(shareOnly = false)
    }

    /**
     * Called by the platform layer after the notification permission result is
     * known (granted or denied). Proceeds with the download regardless of result
     * (notification permission is optional; we show a snackbar but don't block).
     */
    fun onNotificationPermissionResult(granted: Boolean) {
        // On Android, if the user denied the permission, the RequestNotificationPermission
        // event was already emitted before the launcher was shown. We still proceed.
        // [pendingShareOnly] is only valid within this single permission-request window
        // (set by startDownloadWithPermissionCheck); read it once and reset so a stale
        // value can never leak into a later, unrelated flow.
        val shareOnly = pendingShareOnly
        pendingShareOnly = false
        startDownload(shareOnly)
    }

    private fun handleShareFormat() {
        startDownloadWithPermissionCheck(shareOnly = true)
    }

    /**
     * Emits [DownloadEvent.RequestNotificationPermission] so the platform UI
     * can launch the system permission dialog. Called by the Android delegate
     * inside [PlatformDelegate.checkNotificationPermission] when permission is absent.
     */
    fun emitRequestNotificationPermission() {
        coroutineScope.launch {
            _events.send(DownloadEvent.RequestNotificationPermission)
        }
    }

    /**
     * Emits [DownloadEvent.ShowPlatformLogin] so the platform UI can show the login screen.
     * Called by the Android delegate inside [PlatformDelegate.showPlatformLogin].
     */
    fun emitShowPlatformLogin(platform: SupportedPlatform) {
        coroutineScope.launch {
            _events.send(DownloadEvent.ShowPlatformLogin(platform))
        }
    }

    private fun startDownloadWithPermissionCheck(shareOnly: Boolean) {
        pendingShareOnly = shareOnly
        val delegate = platformDelegate
        if (delegate != null) {
            // Platform layer handles permission check and calls back via
            // onNotificationPermissionResult when the permission flow is complete.
            delegate.checkNotificationPermission(shareOnly)
        } else {
            // No delegate (e.g., iOS) — proceed immediately.
            startDownload(shareOnly)
        }
    }

    fun startDownload(shareOnly: Boolean = false) {
        val state = _uiState.value
        if (state !is DownloadUiState.FormatSelection) return

        val selectedFormat =
            state.metadata.formats
                .find { it.formatId == state.selectedFormatId } ?: return
        val requestId = generateUuid()

        val request =
            DownloadRequest(
                id = requestId,
                sourceUrl = state.metadata.sourceUrl,
                videoTitle = state.metadata.title,
                thumbnailUrl = state.metadata.thumbnailUrl,
                formatId = selectedFormat.formatId,
                ext = selectedFormat.ext,
                formatLabel = selectedFormat.label,
                isVideoOnly = selectedFormat.isVideoOnly,
                totalBytes = selectedFormat.fileSizeBytes,
                shareOnly = shareOnly,
                existingRecordId = existingRecordId,
                directDownloadUrl = selectedFormat.directDownloadUrl,
            )

        _uiState.value =
            DownloadUiState.Downloading(
                metadata = state.metadata,
                progress =
                    DownloadProgress(
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

        coroutineScope.launch {
            platformDownloadManager.startDownload(request)
        }
    }

    private fun handleCancel() {
        val state = _uiState.value
        val requestId = (state as? DownloadUiState.Downloading)?.progress?.requestId ?: return
        platformDownloadManager.cancelDownload(requestId)
    }

    private fun handleRetry() {
        val state = _uiState.value
        if (state !is DownloadUiState.Error) return
        when (val action = state.retryAction) {
            is RetryAction.RetryExtraction -> {
                currentUrl = action.url
                handleExtract()
            }
            null -> Unit
        }
    }

    private fun handleOpenFile() {
        val state = _uiState.value
        if (state is DownloadUiState.Done) {
            coroutineScope.launch {
                _events.send(DownloadEvent.OpenFile(state.fileUri ?: state.filePath))
            }
        }
    }

    private fun handleShareFile() {
        val state = _uiState.value
        if (state is DownloadUiState.Done) {
            coroutineScope.launch {
                _events.send(DownloadEvent.ShareFile(state.fileUri ?: state.filePath))
            }
        }
    }

    private fun handleNewDownload() {
        val current = _uiState.value
        if (current is DownloadUiState.Downloading) {
            backgroundDownload = current
        }
        currentUrl = ""
        existingRecordId = null
        duplicateCheckJob?.cancel()
        _uiState.value = DownloadUiState.Idle(connectedPlatforms = secureCookieStore.connectedPlatforms())
    }

    private fun handleBackToIdle() {
        _uiState.value =
            DownloadUiState.Idle(
                prefillUrl = currentUrl,
                connectedPlatforms = secureCookieStore.connectedPlatforms(),
            )
    }

    private fun handleOpenExisting() {
        val state = _uiState.value
        if (state is DownloadUiState.Idle) {
            val existing = state.existingDownload ?: return
            coroutineScope.launch {
                _events.send(DownloadEvent.OpenFile(existing.contentUri))
            }
        }
    }

    private fun handleShareExisting() {
        val state = _uiState.value
        if (state is DownloadUiState.Idle) {
            val existing = state.existingDownload ?: return
            coroutineScope.launch {
                _events.send(DownloadEvent.ShareFile(existing.contentUri))
            }
        }
    }

    private fun handleDismissExistingBanner() {
        val current = _uiState.value
        if (current is DownloadUiState.Idle) {
            _uiState.value =
                DownloadUiState.Idle(
                    prefillUrl = current.prefillUrl,
                    connectedPlatforms = secureCookieStore.connectedPlatforms(),
                )
        }
    }

    private fun handlePrefillUrl(
        url: String,
        recordId: Long? = null,
    ) {
        duplicateCheckJob?.cancel()
        currentUrl = url
        existingRecordId = recordId
        coroutineScope.launch {
            val existing = findExistingDownload(url)
            if (existing != null) {
                _uiState.value =
                    DownloadUiState.Idle(
                        existingDownload = existing,
                        prefillUrl = url,
                        connectedPlatforms = secureCookieStore.connectedPlatforms(),
                    )
            } else {
                handleExtract()
            }
        }
    }

    private fun handleConnectPlatform(platform: SupportedPlatform) {
        val delegate = platformDelegate
        if (delegate != null) {
            delegate.showPlatformLogin(platform)
        } else {
            // iOS path — emit event for shared screen overlay
            coroutineScope.launch {
                _events.send(DownloadEvent.ShowPlatformLogin(platform))
            }
        }
    }

    private fun handlePlatformLoginResult(
        platform: SupportedPlatform,
        success: Boolean,
    ) {
        if (success && currentUrl.isNotBlank()) {
            // Auto-retry extraction after successful login
            duplicateCheckJob?.cancel()
            extractionJob?.cancel()
            val url = currentUrl
            _uiState.value = DownloadUiState.Extracting(url)
            extractionJob =
                coroutineScope.launch {
                    extractWithRetry(url)
                }
        }
    }

    private fun handleDisconnectPlatform(platform: SupportedPlatform) {
        secureCookieStore.clearCookies(platform)
        val current = _uiState.value
        if (current is DownloadUiState.Idle) {
            _uiState.value = current.copy(connectedPlatforms = secureCookieStore.connectedPlatforms())
        }
    }

    /** Cancel the coroutine scope when the ViewModel is cleared. */
    fun cleanup() {
        coroutineScope.cancel()
    }
}

/** Platform-independent UUID generator — expect/actual if needed, for now use UUID string. */
internal expect fun generateUuid(): String
