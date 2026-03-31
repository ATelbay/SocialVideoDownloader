package com.socialvideodownloader.feature.library.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.socialvideodownloader.core.domain.file.FileAccessManager
import com.socialvideodownloader.core.domain.repository.DownloadRepository
import com.socialvideodownloader.shared.feature.library.SharedLibraryViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * Android thin delegate ViewModel for the library screen.
 *
 * All business logic lives in [SharedLibraryViewModel]. This class bridges
 * Hilt DI into the shared KMP ViewModel.
 *
 * The Compose UI layer continues to observe [uiState] and [effect]
 * without any changes — the public API surface is identical to the old ViewModel.
 */
@HiltViewModel
class LibraryViewModel
    @Inject
    constructor(
        downloadRepository: DownloadRepository,
        fileManager: FileAccessManager,
    ) : ViewModel() {
        private val shared =
            SharedLibraryViewModel(
                coroutineScope = viewModelScope,
                downloadRepository = downloadRepository,
                fileManager = fileManager,
            )

        val uiState: StateFlow<LibraryUiState> = shared.uiState
        val effect: Flow<LibraryEffect> = shared.effect

        fun onIntent(intent: LibraryIntent) = shared.onIntent(intent)
    }
