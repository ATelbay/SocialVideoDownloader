package com.socialvideodownloader.feature.library.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.socialvideodownloader.feature.library.R
import com.socialvideodownloader.feature.library.domain.ObserveLibraryItemsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    observeLibraryItems: ObserveLibraryItemsUseCase,
) : ViewModel() {

    private val _effect = Channel<LibraryEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    val uiState: StateFlow<LibraryUiState> = observeLibraryItems()
        .map { items ->
            if (items.isEmpty()) {
                LibraryUiState.Empty
            } else {
                LibraryUiState.Content(items.map { item ->
                    LibraryListItem(
                        id = item.id,
                        title = item.title,
                        formatLabel = item.formatLabel,
                        thumbnailUrl = item.thumbnailUrl,
                        platformName = item.platformName,
                        completedAt = item.completedAt,
                        fileSizeBytes = item.fileSizeBytes,
                        contentUri = item.contentUri,
                    )
                })
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState.Loading)

    fun onIntent(intent: LibraryIntent) {
        when (intent) {
            is LibraryIntent.ItemClicked -> handleItemClicked(intent.itemId)
            is LibraryIntent.ItemLongPressed -> handleItemLongPressed(intent.itemId)
        }
    }

    private fun handleItemClicked(itemId: Long) {
        val items = (uiState.value as? LibraryUiState.Content)?.items ?: return
        val item = items.find { it.id == itemId } ?: run {
            viewModelScope.launch { _effect.send(LibraryEffect.ShowMessage(R.string.library_open_error)) }
            return
        }
        viewModelScope.launch {
            _effect.send(LibraryEffect.OpenContent(item.contentUri))
        }
    }

    private fun handleItemLongPressed(itemId: Long) {
        val items = (uiState.value as? LibraryUiState.Content)?.items ?: return
        val item = items.find { it.id == itemId } ?: return
        viewModelScope.launch {
            _effect.send(LibraryEffect.ShareContent(item.contentUri))
        }
    }
}
