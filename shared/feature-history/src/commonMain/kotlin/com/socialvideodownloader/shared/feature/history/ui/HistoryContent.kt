package com.socialvideodownloader.shared.feature.history.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.socialvideodownloader.shared.feature.history.HistoryIntent
import com.socialvideodownloader.shared.feature.history.HistoryUiState
import com.socialvideodownloader.shared.ui.components.TextActionLink
import com.socialvideodownloader.shared.ui.theme.Spacing
import com.socialvideodownloader.shared.ui.theme.SvdPrimary

@Composable
fun HistoryContent(
    uiState: HistoryUiState,
    emptyTitle: String,
    emptyDescription: String,
    noResultsDescription: String,
    startDownloadingLabel: String,
    startNewDownloadLabel: String,
    formattedDate: (epochMillis: Long) -> String,
    formattedSize: (bytes: Long) -> String,
    onIntent: (HistoryIntent) -> Unit,
    onStartDownloading: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        is HistoryUiState.Loading -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = SvdPrimary)
            }
        }

        is HistoryUiState.Empty -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                HistoryEmptyState(
                    isSearchResult = uiState.isFiltering,
                    onStartDownloading = onStartDownloading,
                    emptyTitle = emptyTitle,
                    emptyDescription = emptyDescription,
                    noResultsDescription = noResultsDescription,
                    startDownloadingLabel = startDownloadingLabel,
                )
            }
        }

        is HistoryUiState.Content -> {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding =
                    PaddingValues(
                        top = Spacing.CardInnerPaddingFull,
                        start = Spacing.ScreenPadding,
                        end = Spacing.ScreenPadding,
                        bottom = 80.dp,
                    ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(uiState.items, key = { it.id }) { item ->
                    HistoryListItemRow(
                        item = item,
                        onClick = { onIntent(HistoryIntent.HistoryItemClicked(item.id)) },
                        onLongClick = { onIntent(HistoryIntent.HistoryItemLongPressed(item.id)) },
                        formattedDate = formattedDate(item.createdAt),
                        formattedSize = item.fileSizeBytes?.let { formattedSize(it) },
                    )
                }
                item {
                    TextActionLink(
                        text = startNewDownloadLabel,
                        onClick = onStartDownloading,
                    )
                }
            }
        }
    }
}
