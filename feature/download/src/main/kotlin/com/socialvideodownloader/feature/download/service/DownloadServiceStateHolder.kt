package com.socialvideodownloader.feature.download.service

import com.socialvideodownloader.core.domain.model.DownloadProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

sealed interface DownloadServiceState {
    data object Idle : DownloadServiceState

    data class Downloading(
        val requestId: String,
        val progress: DownloadProgress,
    ) : DownloadServiceState

    data class Queued(val pendingIds: List<String>) : DownloadServiceState

    data class Completed(val requestId: String, val filePath: String) : DownloadServiceState

    data class Failed(val requestId: String, val error: String) : DownloadServiceState

    data class Cancelled(val requestId: String) : DownloadServiceState
}

@Singleton
class DownloadServiceStateHolder
    @Inject
    constructor() {
        private val _state = MutableStateFlow<DownloadServiceState>(DownloadServiceState.Idle)
        val state: StateFlow<DownloadServiceState> = _state.asStateFlow()

        fun update(newState: DownloadServiceState) {
            _state.value = newState
        }
    }
