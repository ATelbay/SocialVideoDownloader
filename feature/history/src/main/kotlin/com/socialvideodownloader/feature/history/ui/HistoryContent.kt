package com.socialvideodownloader.feature.history.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.socialvideodownloader.feature.history.R

@Composable
fun HistoryContent(
    uiState: HistoryUiState,
    onIntent: (HistoryIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        is HistoryUiState.Loading -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }

        is HistoryUiState.Empty -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (uiState.isFiltering) {
                        stringResource(R.string.history_no_results)
                    } else {
                        stringResource(R.string.history_empty_message)
                    },
                )
            }
        }

        is HistoryUiState.Content -> {
            LazyColumn(modifier = modifier.fillMaxSize()) {
                items(uiState.items, key = { it.id }) { item ->
                    Box {
                        HistoryListItemRow(
                            item = item,
                            onClick = { onIntent(HistoryIntent.HistoryItemClicked(item.id)) },
                            onLongClick = { onIntent(HistoryIntent.HistoryItemLongPressed(item.id)) },
                        )
                        HistoryItemMenu(
                            isVisible = uiState.openMenuItemId == item.id,
                            isFileAccessible = item.isFileAccessible,
                            onShareClick = { onIntent(HistoryIntent.ShareClicked(item.id)) },
                            onDeleteClick = { onIntent(HistoryIntent.DeleteItemClicked(item.id)) },
                            onDismiss = { onIntent(HistoryIntent.DismissItemMenu) },
                        )
                    }
                }
            }
        }
    }
}
