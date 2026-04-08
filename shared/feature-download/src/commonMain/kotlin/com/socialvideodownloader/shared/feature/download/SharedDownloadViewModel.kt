package com.socialvideodownloader.shared.feature.download

import com.socialvideodownloader.core.domain.model.DownloadProgress
import com.socialvideodownloader.core.domain.model.DownloadRequest
import com.socialvideodownloader.core.domain.usecase.ExtractVideoInfoUseCase
import com.socialvideodownloader.core.domain.usecase.FindExistingDownloadUseCase
import com.socialvideodownloader.shared.data.platform.DownloadErrorType
import com.socialvideodownloader.shared.data.platform.DownloadServiceState
import com.socialvideodownloader.shared.data.platform.PlatformDownloadManager
import com.socialvideodownloader.shared.feature.download.ui.DownloadAuthStrings
import com.socialvideodownloader.shared.network.ServerExtractionException
import com.socialvideodownloader.shared.network.auth.CookieStore
import com.socialvideodownloader.shared.network.auth.SupportedPlatform
import com.socialvideodownloader.shared.network.auth.detectPlatform
import com.socialvideodownloader.shared.network.auth.detectPlatformFromError
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
    private var pendingShareOnly: Boolean = false
    private var existingRecordId: Long? = null

    init {
        collectServiceState()
        val url = initialUrl ?: savedUrl
        if (url != null) {
            currentUrl = url
            _uiState.value = DownloadUiState.Idle(connectedPlatforms = secureCookieStore.connectedPlatforms())
        }
    }

    private fun collectServiceState() {
        coroutineScope.launch {
            platformDownloadManager.downloadState.collect { serviceState ->
                val current = _uiState.value
                when (serviceState) {
                    is DownloadServiceState.Downloading -> {
                        if (current is DownloadUiState.Downloading) {
                            _uiState.value = current.copy(progress = serviceState.progress)
                        } else if (current is DownloadUiState.FormatSelection) {
                            _uiState.value =
                                DownloadUiState.Downloading(
                                    metadata = current.metadata,
                                    progress = serviceState.progress,
                                    selectedFormatId = current.selectedFormatId,
                                )
                        }
                    }
                    is DownloadServiceState.Completed -> {
                        val downloading =
                            current as? DownloadUiState.Downloading
                                ?: backgroundDownload?.takeIf { it.progress.requestId == serviceState.requestId }
                                ?: return@collect
                        backgroundDownload = null
                        if (downloading.progress.requestId != serviceState.requestId) return@collect
                        if (downloading.isShareMode) {
                            _events.send(DownloadEvent.ShareFile(serviceState.filePath))
                            _uiState.value =
                                DownloadUiState.FormatSelection(
                                    metadata = downloading.metadata,
                                    selectedFormatId = downloading.selectedFormatId,
                                )
                        } else {
                            _uiState.value =
                                DownloadUiState.Done(
                                    metadata = downloading.metadata,
                                    filePath = serviceState.filePath,
                                    fileUri = serviceState.fileUri,
                                )
                        }
                    }
                    is DownloadServiceState.Failed -> {
                        val downloading =
                            current as? DownloadUiState.Downloading
                                ?: backgroundDownload?.takeIf { it.progress.requestId == serviceState.requestId }
                                ?: return@collect
                        backgroundDownload = null
                        if (downloading.progress.requestId != serviceState.requestId) return@collect
                        if (downloading.isShareMode) {
                            _events.send(
                                DownloadEvent.ShowError(serviceState.error, message = null),
                            )
                            _uiState.value =
                                DownloadUiState.FormatSelection(
                                    metadata = downloading.metadata,
                                    selectedFormatId = downloading.selectedFormatId,
                                )
                        } else {
                            _uiState.value =
                                DownloadUiState.Error(
                                    errorType = serviceState.error,
                                    message = null,
                                    retryAction = RetryAction.RetryExtraction(currentUrl),
                                )
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
                        }
                    }
                    is DownloadServiceState.Idle -> Unit
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
                    _uiState.value =
                        DownloadUiState.Idle(
                            existingDownload = existing,
                            connectedPlatforms = secureCookieStore.connectedPlatforms(),
                        )
                }
            }
    }

    private fun handleExtract() {
        if (currentUrl.isBlank()) return
        duplicateCheckJob?.cancel()
        _uiState.value = DownloadUiState.Extracting(currentUrl)

        coroutineScope.launch {
            extractWithRetry(currentUrl)
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
                    val bestFormatId =
                        metadata.formats
                            .firstOrNull { !it.isAudioOnly }?.formatId
                            ?: metadata.formats.firstOrNull()?.formatId
                            ?: ""
                    _uiState.value =
                        DownloadUiState.FormatSelection(
                            metadata = metadata,
                            selectedFormatId = bestFormatId,
                        )
                    return
                }
                .onFailure { error ->
                    if (error is CancellationException) throw error

                    val errorType = mapErrorToType(error)
                    val isTransient = isTransientError(errorType)

                    if (isTransient && attempt < maxRetryAttempts) {
                        attempt++
                        val backoffMs = retryBaseDelayMs * (1L shl (attempt - 1)) // 1s, 2s, 4s
                        delay(backoffMs)
                        // Only retry if still in Extracting state (user hasn't navigated away)
                        if (_uiState.value !is DownloadUiState.Extracting) return
                    } else {
                        val platform = if (errorType == DownloadErrorType.AUTH_REQUIRED) detectPlatform(url) else null
                        // If cookies exist but extraction still failed, show "Reconnect" label
                        // but DON'T clear cookies — they may still be valid for retry.
                        // Cookies are only replaced when the user completes a new login.
                        val isReconnect = platform != null && secureCookieStore.isConnected(platform)
                        _uiState.value =
                            DownloadUiState.Error(
                                errorType = errorType,
                                message = friendlyErrorMessage(error),
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
        startDownload(pendingShareOnly)
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
                _events.send(DownloadEvent.OpenFile(state.filePath))
            }
        }
    }

    private fun handleShareFile() {
        val state = _uiState.value
        if (state is DownloadUiState.Done) {
            coroutineScope.launch {
                _events.send(DownloadEvent.ShareFile(state.filePath))
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
            _uiState.value = DownloadUiState.Extracting(currentUrl)
            coroutineScope.launch {
                extractWithRetry(currentUrl)
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

    private fun friendlyErrorMessage(error: Throwable): String {
        val raw = error.message ?: return "An unexpected error occurred"
        val lower = raw.lowercase()

        // Auth-required: platform-specific message
        val platform = detectPlatform(currentUrl)
        if (platform != null) {
            val authKeywords =
                listOf("sign in", "login required", "must be logged in", "inappropriate", "age-restricted", "age restricted", "nsfw")
            if (authKeywords.any { lower.contains(it) }) {
                return DownloadAuthStrings.authRequiredMessage(platform.displayName)
            }
        }

        return when {
            lower.contains("private video") || lower.contains("is private") ->
                "This video is private and cannot be accessed."
            lower.contains("not available") || lower.contains("not found") ||
                lower.contains("been removed") || lower.contains("been deleted") ->
                "This video is unavailable. It may have been removed or is not available in your region."
            lower.contains("copyright") ->
                "This video is blocked due to a copyright claim."
            lower.contains("unsupported url") ->
                "This URL is not supported. Please try a different link."
            else -> raw
        }
    }

    private fun mapErrorToType(error: Throwable): DownloadErrorType {
        val message = error.message ?: return DownloadErrorType.UNKNOWN

        // Check for auth-required errors on supported platforms (before ServerExtractionException
        // short-circuit, because the WS proxy wraps yt-dlp auth errors as ServerExtractionException)
        val authKeywords =
            listOf("sign in", "login required", "must be logged in", "inappropriate", "age-restricted", "age restricted", "nsfw")
        val lower = message.lowercase()
        if (authKeywords.any { lower.contains(it) } && detectPlatform(currentUrl) != null) {
            return DownloadErrorType.AUTH_REQUIRED
        }

        // Fallback: if yt-dlp error has a platform tag (e.g. [youtube]) matching the URL's
        // platform, offer auth as a recovery option — the extractor error may be auth-gated
        val platformFromUrl = detectPlatform(currentUrl)
        val platformFromError = detectPlatformFromError(message)
        if (platformFromUrl != null && platformFromUrl == platformFromError) {
            return DownloadErrorType.AUTH_REQUIRED
        }

        if (error is ServerExtractionException) {
            return DownloadErrorType.EXTRACTION_FAILED
        }

        return when {
            message.contains("Unsupported URL", ignoreCase = true) -> DownloadErrorType.UNSUPPORTED_URL
            // Server unreachable / backend down (check before generic "unavailable")
            message.contains("Connection refused", ignoreCase = true) ||
                message.contains("ECONNREFUSED", ignoreCase = true) ||
                message.contains("server error", ignoreCase = true) ||
                message.contains("502", ignoreCase = true) ||
                message.contains("503", ignoreCase = true) ||
                message.contains("504", ignoreCase = true) -> DownloadErrorType.SERVER_UNAVAILABLE
            message.contains("unavailable", ignoreCase = true) ||
                message.contains("private", ignoreCase = true) -> DownloadErrorType.EXTRACTION_FAILED
            message.contains("network", ignoreCase = true) ||
                message.contains("connect", ignoreCase = true) ||
                message.contains("timed out", ignoreCase = true) ||
                message.contains("timeout", ignoreCase = true) ||
                message.contains("internet", ignoreCase = true) -> DownloadErrorType.NETWORK_ERROR
            message.contains("space", ignoreCase = true) ||
                message.contains("storage", ignoreCase = true) -> DownloadErrorType.STORAGE_FULL
            else -> DownloadErrorType.UNKNOWN
        }
    }
}

/** Platform-independent UUID generator — expect/actual if needed, for now use UUID string. */
internal expect fun generateUuid(): String
