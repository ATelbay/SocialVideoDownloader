package com.socialvideodownloader.feature.history.ui

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.socialvideodownloader.core.ui.components.TextActionLink
import com.socialvideodownloader.core.ui.theme.SvdPrimary
import com.socialvideodownloader.core.ui.tokens.Spacing
import com.socialvideodownloader.feature.history.R
import com.socialvideodownloader.feature.history.components.HistoryEmptyState

@Composable
fun HistoryContent(
    uiState: HistoryUiState,
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
                )
            }
        }

        is HistoryUiState.Content -> {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = Spacing.ContentTopPadding,
                    start = Spacing.ScreenPadding,
                    end = Spacing.ScreenPadding,
                    bottom = Spacing.ContentTopPadding,
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(uiState.items, key = { it.id }) { item ->
                    HistoryListItemRow(
                        item = item,
                        onClick = { onIntent(HistoryIntent.HistoryItemClicked(item.id)) },
                        onLongClick = { onIntent(HistoryIntent.HistoryItemLongPressed(item.id)) },
                    )
                }
                item {
                    TextActionLink(
                        text = stringResource(R.string.history_start_new_download),
                        onClick = onStartDownloading,
                    )
                }
            }
        }
    }
}
