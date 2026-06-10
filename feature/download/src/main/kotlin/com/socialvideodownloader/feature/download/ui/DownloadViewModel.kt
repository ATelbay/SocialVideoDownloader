package com.socialvideodownloader.feature.download.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.socialvideodownloader.core.domain.di.IoDispatcher
import com.socialvideodownloader.core.domain.usecase.ExtractVideoInfoUseCase
import com.socialvideodownloader.core.domain.usecase.FindExistingDownloadUseCase
import com.socialvideodownloader.feature.download.service.DownloadService
import com.socialvideodownloader.feature.download.service.DownloadServiceState
import com.socialvideodownloader.feature.download.service.DownloadServiceStateHolder
import com.socialvideodownloader.shared.data.platform.AndroidDownloadManager
import com.socialvideodownloader.shared.feature.download.DownloadEvent
import com.socialvideodownloader.shared.feature.download.DownloadIntent
import com.socialvideodownloader.shared.feature.download.DownloadUiState
import com.socialvideodownloader.shared.feature.download.SharedDownloadViewModel
import com.socialvideodownloader.shared.network.auth.CookieStore
import com.socialvideodownloader.shared.network.auth.SupportedPlatform
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.socialvideodownloader.shared.data.platform.DownloadServiceState as SharedDownloadServiceState

/**
 * Android thin delegate ViewModel for the download screen.
 *
 * All business logic lives in [SharedDownloadViewModel]. This class bridges:
 * - Android notification permission checks (OS-level API)
 * - [DownloadServiceStateHolder] → [AndroidDownloadManager] state sync
 * - Hilt DI into the shared KMP ViewModel
 * - [SavedStateHandle] for URL persistence across process death
 *
 * The Compose UI layer continues to observe [uiState] and [events] without
 * any changes — the public API surface is identical to the old ViewModel.
 */
@HiltViewModel
class DownloadViewModel
    @Inject
    constructor(
        private val extractVideoInfo: ExtractVideoInfoUseCase,
        private val findExistingDownload: FindExistingDownloadUseCase,
        private val serviceStateHolder: DownloadServiceStateHolder,
        @ApplicationContext private val context: Context,
        private val savedStateHandle: SavedStateHandle,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
        private val androidDownloadManager: AndroidDownloadManager,
        private val secureCookieStore: CookieStore,
    ) : ViewModel() {
        val shared =
            SharedDownloadViewModel(
                coroutineScope = viewModelScope,
                extractVideoInfo = extractVideoInfo,
                findExistingDownload = findExistingDownload,
                platformDownloadManager = androidDownloadManager,
                secureCookieStore = secureCookieStore,
                initialUrl = savedStateHandle["initialUrl"],
                savedUrl = savedStateHandle["currentUrl"],
            )

        val uiState: StateFlow<DownloadUiState> = shared.uiState
        val events: Flow<DownloadEvent> = shared.events

        /** Separate channel for Android-only navigation to platform login screen. */
        private val _platformLoginNav = Channel<SupportedPlatform>(Channel.BUFFERED)
        val platformLoginNav = _platformLoginNav.receiveAsFlow()

        init {
            // Wire notification permission check back through the Android layer
            shared.platformDelegate =
                object : SharedDownloadViewModel.PlatformDelegate {
                    override fun checkNotificationPermission(pendingShareOnly: Boolean) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            val hasPermission =
                                ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS,
                                ) == PackageManager.PERMISSION_GRANTED
                            if (!hasPermission) {
                                // Ask the shared VM to emit RequestNotificationPermission event.
                                // The UI will launch the system permission dialog; after the user
                                // responds, the composable calls viewModel.onNotificationPermissionResult().
                                shared.emitRequestNotificationPermission()
                                return
                            }
                        }
                        shared.startDownload(shareOnly = pendingShareOnly)
                    }

                    override fun showPlatformLogin(platform: SupportedPlatform) {
                        // Use a separate Android-only channel to avoid competing with
                        // the shared DownloadScreen's event collector (which would trigger
                        // the iOS overlay and crash on the Android no-op actual).
                        viewModelScope.launch {
                            _platformLoginNav.send(platform)
                        }
                    }
                }

            // Sync DownloadServiceStateHolder → AndroidDownloadManager on every state change.
            // This bridges the Android foreground service's state into the shared VM.
            viewModelScope.launch {
                serviceStateHolder.state.collect { serviceState ->
                    androidDownloadManager.updateState(mapServiceState(serviceState))
                }
            }
        }

        fun onIntent(intent: DownloadIntent) {
            // Persist the URL changes via SavedStateHandle for process-death survival.
            if (intent is DownloadIntent.UrlChanged) {
                savedStateHandle["currentUrl"] = intent.url
            }
            shared.onIntent(intent)
        }

        /**
         * Called by the Activity/composable after the POST_NOTIFICATIONS permission result.
         * Forwards to the shared VM which decides whether to proceed or show rationale.
         */
        fun onNotificationPermissionResult(granted: Boolean) {
            shared.onNotificationPermissionResult(granted)
        }

        override fun onCleared() {
            // Only sweep leftover share temp files when the service is not active.
            // Detached from viewModelScope (already cancelled here); self-cancels once the
            // one-shot cleanup finishes so the scope doesn't linger.
            if (serviceStateHolder.state.value is DownloadServiceState.Idle) {
                val cleanupScope = CoroutineScope(ioDispatcher)
                cleanupScope.launch {
                    try {
                        java.io.File(context.cacheDir, DownloadService.SHARE_TEMP_DIR).deleteRecursively()
                    } finally {
                        cleanupScope.cancel()
                    }
                }
            }
            // Do NOT call shared.cleanup() — viewModelScope is cancelled by super.onCleared(),
            // and SharedDownloadViewModel uses the same scope.
            super.onCleared()
        }

        private fun mapServiceState(serviceState: DownloadServiceState): SharedDownloadServiceState {
            return when (serviceState) {
                is DownloadServiceState.Idle -> SharedDownloadServiceState.Idle
                is DownloadServiceState.Queued ->
                    // The shared VM ignores Queued (no state change), so the title placeholder
                    // is never read — the service queue only tracks request IDs.
                    SharedDownloadServiceState.Queued(
                        requestId = serviceState.pendingIds.firstOrNull() ?: "",
                        videoTitle = "",
                    )
                is DownloadServiceState.Downloading ->
                    SharedDownloadServiceState.Downloading(
                        requestId = serviceState.requestId,
                        progress = serviceState.progress,
                    )
                is DownloadServiceState.Completed ->
                    // The service emits a single content URI (MediaStore or FileProvider);
                    // it belongs in the fileUri slot, with filePath as the required fallback.
                    SharedDownloadServiceState.Completed(
                        requestId = serviceState.requestId,
                        filePath = serviceState.filePath,
                        fileUri = serviceState.filePath,
                    )
                is DownloadServiceState.Failed ->
                    SharedDownloadServiceState.Failed(
                        requestId = serviceState.requestId,
                        error = serviceState.errorType,
                    )
                is DownloadServiceState.Cancelled ->
                    SharedDownloadServiceState.Cancelled(
                        requestId = serviceState.requestId,
                    )
            }
        }
    }

// Re-export DownloadEvent as a typealias so the existing Compose UI layer
// can import it from com.socialvideodownloader.feature.download.ui without changes.
typealias DownloadEvent = com.socialvideodownloader.shared.feature.download.DownloadEvent
