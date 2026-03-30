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
import com.socialvideodownloader.feature.download.service.DownloadService
import com.socialvideodownloader.feature.download.service.DownloadServiceState
import com.socialvideodownloader.feature.download.service.DownloadServiceStateHolder
import com.socialvideodownloader.shared.data.platform.AndroidDownloadManager
import com.socialvideodownloader.shared.data.platform.DownloadServiceState as SharedDownloadServiceState
import com.socialvideodownloader.core.domain.usecase.ExtractVideoInfoUseCase
import com.socialvideodownloader.core.domain.usecase.FindExistingDownloadUseCase
import com.socialvideodownloader.shared.feature.download.DownloadEvent
import com.socialvideodownloader.shared.feature.download.DownloadIntent
import com.socialvideodownloader.shared.feature.download.DownloadUiState
import com.socialvideodownloader.shared.feature.download.SharedDownloadViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

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
class DownloadViewModel @Inject constructor(
    private val extractVideoInfo: ExtractVideoInfoUseCase,
    private val findExistingDownload: FindExistingDownloadUseCase,
    private val serviceStateHolder: DownloadServiceStateHolder,
    @ApplicationContext private val context: Context,
    private val savedStateHandle: SavedStateHandle,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val androidDownloadManager: AndroidDownloadManager,
) : ViewModel() {

    private val shared = SharedDownloadViewModel(
        coroutineScope = viewModelScope,
        extractVideoInfo = extractVideoInfo,
        findExistingDownload = findExistingDownload,
        platformDownloadManager = androidDownloadManager,
        initialUrl = savedStateHandle["initialUrl"],
        savedUrl = savedStateHandle["currentUrl"],
    )

    val uiState: StateFlow<DownloadUiState> = shared.uiState
    val events: Flow<DownloadEvent> = shared.events

    init {
        // Wire notification permission check back through the Android layer
        shared.platformDelegate = object : SharedDownloadViewModel.PlatformDelegate {
            override fun checkNotificationPermission(pendingShareOnly: Boolean) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val hasPermission = ContextCompat.checkSelfPermission(
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
        if (serviceStateHolder.state.value is DownloadServiceState.Idle) {
            CoroutineScope(ioDispatcher).launch {
                java.io.File(context.cacheDir, DownloadService.SHARE_TEMP_DIR).deleteRecursively()
            }
        }
        // Do NOT call shared.cleanup() — viewModelScope is cancelled by super.onCleared(),
        // and SharedDownloadViewModel uses the same scope.
        super.onCleared()
    }

    private fun mapServiceState(serviceState: DownloadServiceState): SharedDownloadServiceState {
        return when (serviceState) {
            is DownloadServiceState.Idle -> SharedDownloadServiceState.Idle
            is DownloadServiceState.Queued -> SharedDownloadServiceState.Queued(
                requestId = serviceState.pendingIds.firstOrNull() ?: "",
                videoTitle = "",
            )
            is DownloadServiceState.Downloading -> SharedDownloadServiceState.Downloading(
                requestId = serviceState.requestId,
                progress = serviceState.progress,
            )
            is DownloadServiceState.Completed -> SharedDownloadServiceState.Completed(
                requestId = serviceState.requestId,
                filePath = serviceState.filePath,
            )
            is DownloadServiceState.Failed -> SharedDownloadServiceState.Failed(
                requestId = serviceState.requestId,
                error = com.socialvideodownloader.shared.data.platform.DownloadErrorType.DOWNLOAD_FAILED,
            )
            is DownloadServiceState.Cancelled -> SharedDownloadServiceState.Cancelled(
                requestId = serviceState.requestId,
            )
        }
    }
}

// Re-export DownloadEvent as a typealias so the existing Compose UI layer
// can import it from com.socialvideodownloader.feature.download.ui without changes.
typealias DownloadEvent = com.socialvideodownloader.shared.feature.download.DownloadEvent
