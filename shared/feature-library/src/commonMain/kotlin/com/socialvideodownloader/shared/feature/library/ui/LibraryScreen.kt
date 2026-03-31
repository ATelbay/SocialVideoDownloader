package com.socialvideodownloader.shared.feature.library.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.socialvideodownloader.shared.feature.library.LibraryEffect
import com.socialvideodownloader.shared.feature.library.LibraryIntent
import com.socialvideodownloader.shared.feature.library.LibraryMessageType
import com.socialvideodownloader.shared.feature.library.LibraryUiState
import com.socialvideodownloader.shared.feature.library.SharedLibraryViewModel
import com.socialvideodownloader.shared.feature.library.platform.rememberPlatformActions
import com.socialvideodownloader.shared.ui.components.SvdTopBar
import com.socialvideodownloader.shared.ui.theme.SvdBg
import com.socialvideodownloader.shared.ui.theme.SvdPrimary
import com.socialvideodownloader.shared.ui.theme.Spacing

@Composable
fun LibraryScreen(
    viewModel: SharedLibraryViewModel,
    onNavigateToDownload: () -> Unit,
    strings: LibraryStrings,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val platformActions = rememberPlatformActions()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            snackbarHostState.currentSnackbarData?.dismiss()
            when (effect) {
                is LibraryEffect.OpenContent -> {
                    try {
                        platformActions.openFile(effect.contentUri)
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar(strings.openError)
                    }
                }
                is LibraryEffect.ShareContent -> {
                    try {
                        platformActions.shareFile(effect.contentUri)
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar(strings.shareError)
                    }
                }
                is LibraryEffect.ShowMessage -> {
                    val msg =
                        when (effect.messageType) {
                            LibraryMessageType.DELETE_SUCCESS -> strings.deleted
                            LibraryMessageType.FILE_NOT_FOUND -> strings.openError
                            LibraryMessageType.SHARE_ERROR -> strings.shareError
                            LibraryMessageType.OPEN_ERROR -> strings.openError
                        }
                    snackbarHostState.showSnackbar(msg)
                }
            }
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = SvdBg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            SvdTopBar(title = strings.screenTitle)
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        when (val state = uiState) {
            is LibraryUiState.Loading -> {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = SvdPrimary)
                }
            }

            is LibraryUiState.Empty -> {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    LibraryEmptyState(
                        title = strings.emptyTitle,
                        description = strings.emptyDescription,
                        startDownloadingLabel = strings.startDownloading,
                        onNavigateToDownload = onNavigateToDownload,
                    )
                }
            }

            is LibraryUiState.Content -> {
                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                    contentPadding =
                        PaddingValues(
                            top = Spacing.ItemSpacing,
                            start = Spacing.ScreenPadding,
                            end = Spacing.ScreenPadding,
                            bottom = 80.dp,
                        ),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(state.items, key = { it.id }) { item ->
                        LibraryListItemRow(
                            item = item,
                            onClick = { viewModel.onIntent(LibraryIntent.ItemClicked(item.id)) },
                            onLongClick = { viewModel.onIntent(LibraryIntent.ItemLongPressed(item.id)) },
                        )
                    }
                }
            }
        }
    }
}

data class LibraryStrings(
    val screenTitle: String,
    val emptyTitle: String,
    val emptyDescription: String,
    val startDownloading: String,
    val openError: String,
    val shareError: String,
    val deleted: String,
)
