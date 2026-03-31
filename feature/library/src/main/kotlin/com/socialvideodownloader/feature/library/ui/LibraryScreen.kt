package com.socialvideodownloader.feature.library.ui

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.socialvideodownloader.core.ui.components.SvdTopBar
import com.socialvideodownloader.core.ui.theme.SvdBg
import com.socialvideodownloader.core.ui.theme.SvdPrimary
import com.socialvideodownloader.core.ui.tokens.Spacing
import com.socialvideodownloader.core.ui.util.openVideo
import com.socialvideodownloader.core.ui.util.shareVideo
import com.socialvideodownloader.feature.library.R
import com.socialvideodownloader.feature.library.ui.components.LibraryEmptyState
import com.socialvideodownloader.feature.library.ui.components.LibraryListItemRow
import com.socialvideodownloader.shared.feature.library.LibraryEffect
import com.socialvideodownloader.shared.feature.library.LibraryIntent
import com.socialvideodownloader.shared.feature.library.LibraryMessageType
import com.socialvideodownloader.shared.feature.library.LibraryUiState

@Composable
fun LibraryScreen(
    onNavigateToDownload: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            snackbarHostState.currentSnackbarData?.dismiss()
            when (effect) {
                is LibraryEffect.OpenContent -> {
                    try {
                        context.openVideo(effect.contentUri)
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar(context.getString(R.string.library_open_error))
                    }
                }
                is LibraryEffect.ShareContent -> {
                    try {
                        context.shareVideo(effect.contentUri)
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar(context.getString(R.string.library_share_error))
                    }
                }
                is LibraryEffect.ShowMessage -> {
                    val msgRes =
                        when (effect.messageType) {
                            LibraryMessageType.DELETE_SUCCESS -> R.string.library_deleted
                            LibraryMessageType.FILE_NOT_FOUND -> R.string.library_open_error
                            LibraryMessageType.SHARE_ERROR -> R.string.library_share_error
                            LibraryMessageType.OPEN_ERROR -> R.string.library_open_error
                        }
                    snackbarHostState.showSnackbar(context.getString(msgRes))
                }
            }
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = SvdBg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            SvdTopBar(title = stringResource(R.string.library_screen_title))
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
                    LibraryEmptyState(onNavigateToDownload = onNavigateToDownload)
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
                            top = Spacing.ContentTopPadding,
                            start = Spacing.ScreenPadding,
                            end = Spacing.ScreenPadding,
                            bottom = Spacing.ContentBottomPadding,
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
