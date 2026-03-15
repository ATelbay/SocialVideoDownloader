package com.socialvideodownloader.feature.history.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.socialvideodownloader.core.domain.model.DownloadStatus
import com.socialvideodownloader.feature.history.R
import com.socialvideodownloader.feature.history.domain.DeleteAllHistoryUseCase
import com.socialvideodownloader.feature.history.domain.DeleteHistoryItemUseCase
import com.socialvideodownloader.feature.history.domain.ObserveHistoryItemsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val observeHistoryItems: ObserveHistoryItemsUseCase,
    private val deleteHistoryItem: DeleteHistoryItemUseCase,
    private val deleteAllHistory: DeleteAllHistoryUseCase,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _openMenuItemId = MutableStateFlow<Long?>(null)
    private val _deleteConfirmation = MutableStateFlow<DeleteConfirmationState?>(null)

    private val _effect = MutableSharedFlow<HistoryEffect>()
    val effect: SharedFlow<HistoryEffect> = _effect.asSharedFlow()

    // Keep a reference to all items for delete operations that need access to the full list
    private val _allItems = MutableStateFlow<List<HistoryListItem>>(emptyList())

    val uiState: StateFlow<HistoryUiState> = combine(
        observeHistoryItems(),
        _searchQuery,
        _openMenuItemId,
        _deleteConfirmation,
    ) { allItems, query, openMenuItemId, deleteConfirmation ->
        _allItems.value = allItems
        val trimmedQuery = query.trim()
        if (allItems.isEmpty()) {
            HistoryUiState.Empty(query = trimmedQuery, isFiltering = false)
        } else {
            val filtered = if (trimmedQuery.isBlank()) allItems
                else allItems.filter { it.title.contains(trimmedQuery, ignoreCase = true) }
            if (filtered.isEmpty()) {
                HistoryUiState.Empty(query = trimmedQuery, isFiltering = true)
            } else {
                HistoryUiState.Content(
                    query = trimmedQuery,
                    items = filtered,
                    openMenuItemId = openMenuItemId,
                    deleteConfirmation = deleteConfirmation,
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState.Loading)

    fun onIntent(intent: HistoryIntent) {
        when (intent) {
            is HistoryIntent.SearchQueryChanged -> _searchQuery.value = intent.query
            is HistoryIntent.HistoryItemClicked -> handleItemClicked(intent.itemId)
            is HistoryIntent.ShareClicked -> handleShareClicked(intent.itemId)
            is HistoryIntent.HistoryItemLongPressed -> _openMenuItemId.value = intent.itemId
            is HistoryIntent.DismissItemMenu -> _openMenuItemId.value = null
            is HistoryIntent.DeleteItemClicked -> handleDeleteItemClicked(intent.itemId)
            is HistoryIntent.DeleteAllClicked -> handleDeleteAllClicked()
            is HistoryIntent.DeleteFilesSelectionChanged -> handleDeleteFilesSelectionChanged(intent.selected)
            is HistoryIntent.ConfirmDeletion -> handleConfirmDeletion()
            is HistoryIntent.DismissDeletionDialog -> _deleteConfirmation.value = null
        }
    }

    private fun handleItemClicked(itemId: Long) {
        val item = (uiState.value as? HistoryUiState.Content)?.items?.find { it.id == itemId } ?: return
        viewModelScope.launch {
            if (item.status == DownloadStatus.COMPLETED && item.isFileAccessible) {
                _effect.emit(HistoryEffect.OpenContent(item.contentUri!!))
            } else {
                _effect.emit(HistoryEffect.ShowMessage(R.string.history_file_unavailable))
            }
        }
    }

    private fun handleShareClicked(itemId: Long) {
        val item = (uiState.value as? HistoryUiState.Content)?.items?.find { it.id == itemId } ?: return
        viewModelScope.launch {
            if (item.isFileAccessible) {
                _effect.emit(HistoryEffect.ShareContent(item.contentUri!!))
            } else {
                _effect.emit(HistoryEffect.ShowMessage(R.string.history_file_unavailable))
            }
        }
    }

    private fun handleDeleteItemClicked(itemId: Long) {
        val item = _allItems.value.find { it.id == itemId } ?: return
        _deleteConfirmation.value = DeleteConfirmationState(
            target = DeleteTarget.Single(itemId),
            hasAnyAccessibleFile = item.isFileAccessible,
            affectedCount = 1,
        )
    }

    private fun handleDeleteAllClicked() {
        val allItems = _allItems.value
        _deleteConfirmation.value = DeleteConfirmationState(
            target = DeleteTarget.All,
            hasAnyAccessibleFile = allItems.any { it.isFileAccessible },
            affectedCount = allItems.size,
        )
    }

    private fun handleDeleteFilesSelectionChanged(selected: Boolean) {
        val current = _deleteConfirmation.value ?: return
        _deleteConfirmation.value = current.copy(deleteFilesSelected = selected)
    }

    private fun handleConfirmDeletion() {
        val confirmation = _deleteConfirmation.value ?: return
        viewModelScope.launch {
            when (val target = confirmation.target) {
                is DeleteTarget.Single -> {
                    val result = deleteHistoryItem(target.itemId, confirmation.deleteFilesSelected)
                    if (result.fileDeleteFailed) {
                        _effect.emit(HistoryEffect.ShowMessage(R.string.history_delete_single_file_failed))
                    }
                }
                is DeleteTarget.All -> {
                    val result = deleteAllHistory(deleteFiles = confirmation.deleteFilesSelected)
                    if (result.failedFileDeletions > 0) {
                        _effect.emit(HistoryEffect.ShowMessage(R.string.history_delete_file_cleanup_failed))
                    }
                }
            }
            _deleteConfirmation.value = null
        }
    }
}
