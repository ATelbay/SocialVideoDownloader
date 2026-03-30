package com.socialvideodownloader.shared.feature.library

import com.socialvideodownloader.core.domain.file.FileAccessManager
import com.socialvideodownloader.core.domain.model.LibraryItem
import com.socialvideodownloader.core.domain.repository.DownloadRepository
import com.socialvideodownloader.core.domain.util.PlatformNameResolver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Platform-independent shared ViewModel for the library screen.
 *
 * Contains all state machine logic extracted from the Android LibraryViewModel.
 */
class SharedLibraryViewModel(
    private val coroutineScope: CoroutineScope,
    private val downloadRepository: DownloadRepository,
    private val fileManager: FileAccessManager,
) {
    private val _effect = Channel<LibraryEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    val uiState: StateFlow<LibraryUiState> =
        downloadRepository.getCompletedDownloads()
            .map { records ->
                val items: List<LibraryItem> =
                    records.mapNotNull { record ->
                        val contentUri = fileManager.resolveContentUri(record) ?: return@mapNotNull null
                        if (!fileManager.isFileAccessible(contentUri)) return@mapNotNull null
                        LibraryItem(
                            id = record.id,
                            title = record.videoTitle,
                            formatLabel = record.formatLabel.ifBlank { null },
                            thumbnailUrl = record.thumbnailUrl,
                            platformName = PlatformNameResolver.nameFromUrl(record.sourceUrl) ?: "",
                            completedAt = record.completedAt ?: record.createdAt,
                            fileSizeBytes = record.fileSizeBytes,
                            contentUri = contentUri,
                        )
                    }
                if (items.isEmpty()) {
                    LibraryUiState.Empty
                } else {
                    LibraryUiState.Content(
                        items.map { item ->
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
                        },
                    )
                }
            }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState.Loading)

    fun onIntent(intent: LibraryIntent) {
        when (intent) {
            is LibraryIntent.ItemClicked -> handleItemClicked(intent.itemId)
            is LibraryIntent.ItemLongPressed -> handleItemLongPressed(intent.itemId)
        }
    }

    private fun handleItemClicked(itemId: Long) {
        val items = (uiState.value as? LibraryUiState.Content)?.items ?: return
        val item =
            items.find { it.id == itemId } ?: run {
                coroutineScope.launch { _effect.send(LibraryEffect.ShowMessage(LibraryMessageType.OPEN_ERROR)) }
                return
            }
        coroutineScope.launch {
            _effect.send(LibraryEffect.OpenContent(item.contentUri))
        }
    }

    private fun handleItemLongPressed(itemId: Long) {
        val items = (uiState.value as? LibraryUiState.Content)?.items ?: return
        val item = items.find { it.id == itemId } ?: return
        coroutineScope.launch {
            _effect.send(LibraryEffect.ShareContent(item.contentUri))
        }
    }

    /** Cancel the coroutine scope when the ViewModel is cleared. */
    fun cleanup() {
        coroutineScope.cancel()
    }
}
