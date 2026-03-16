package com.socialvideodownloader.feature.history.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.socialvideodownloader.core.ui.theme.AppShapesInstance
import com.socialvideodownloader.feature.history.R
import com.socialvideodownloader.feature.history.components.HistoryBottomSheet
import com.socialvideodownloader.feature.history.components.HistoryDeleteDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    var isOverflowMenuOpen by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            snackbarHostState.currentSnackbarData?.dismiss()
            when (effect) {
                is HistoryEffect.ShowMessage -> {
                    snackbarHostState.showSnackbar(context.getString(effect.messageResId))
                }
                is HistoryEffect.OpenContent -> {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(Uri.parse(effect.contentUri), "video/*")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar(context.getString(R.string.history_open_error))
                    }
                }
                is HistoryEffect.ShareContent -> {
                    try {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "video/*"
                            putExtra(Intent.EXTRA_STREAM, Uri.parse(effect.contentUri))
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(
                            Intent.createChooser(shareIntent, null).apply {
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            },
                        )
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar(context.getString(R.string.history_share_error))
                    }
                }
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                ),
            ) {
                TopAppBar(
                    title = {
                        if (isSearchActive) {
                            TextField(
                                value = searchQuery,
                                onValueChange = { query ->
                                    searchQuery = query
                                    viewModel.onIntent(HistoryIntent.SearchQueryChanged(query))
                                },
                                placeholder = { Text(stringResource(R.string.history_search_hint)) },
                                singleLine = true,
                                shape = AppShapesInstance.medium,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    disabledIndicatorColor = Color.Transparent,
                                ),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        } else {
                            Text(stringResource(R.string.history_screen_title_full))
                        }
                    },
                    navigationIcon = {
                        if (isSearchActive) {
                            IconButton(onClick = {
                                searchQuery = ""
                                isSearchActive = false
                                viewModel.onIntent(HistoryIntent.SearchQueryChanged(""))
                            }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = null,
                                )
                            }
                        }
                    },
                    actions = {
                        if (!isSearchActive) {
                            IconButton(onClick = { isSearchActive = true }) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = stringResource(R.string.history_search_hint),
                                )
                            }
                            if (uiState is HistoryUiState.Content) {
                                Box {
                                    IconButton(onClick = { isOverflowMenuOpen = true }) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = null,
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = isOverflowMenuOpen,
                                        onDismissRequest = { isOverflowMenuOpen = false },
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                        shape = AppShapesInstance.medium,
                                    ) {
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = stringResource(R.string.history_action_delete_all),
                                                    color = MaterialTheme.colorScheme.error,
                                                )
                                            },
                                            onClick = {
                                                isOverflowMenuOpen = false
                                                viewModel.onIntent(HistoryIntent.DeleteAllClicked)
                                            },
                                            colors = MenuDefaults.itemColors(
                                                textColor = MaterialTheme.colorScheme.error,
                                            ),
                                        )
                                    }
                                }
                            }
                        } else {
                            IconButton(onClick = {
                                searchQuery = ""
                                isSearchActive = false
                                viewModel.onIntent(HistoryIntent.SearchQueryChanged(""))
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        HistoryContent(
            uiState = uiState,
            onIntent = viewModel::onIntent,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        )

        val contentState = uiState as? HistoryUiState.Content
        val openItemId = contentState?.openMenuItemId
        if (openItemId != null) {
            val selectedItem = contentState.items.find { it.id == openItemId }
            if (selectedItem != null) {
                HistoryBottomSheet(
                    title = selectedItem.title,
                    onShare = { viewModel.onIntent(HistoryIntent.ShareClicked(openItemId)) },
                    onDelete = { viewModel.onIntent(HistoryIntent.DeleteItemClicked(openItemId)) },
                    onDismiss = { viewModel.onIntent(HistoryIntent.DismissItemMenu) },
                )
            }
        }

        contentState?.deleteConfirmation?.let { confirmation ->
            HistoryDeleteDialog(
                state = confirmation,
                onDeleteFilesSelectionChanged = { selected ->
                    viewModel.onIntent(HistoryIntent.DeleteFilesSelectionChanged(selected))
                },
                onConfirm = {
                    viewModel.onIntent(HistoryIntent.ConfirmDeletion)
                },
                onDismiss = {
                    viewModel.onIntent(HistoryIntent.DismissDeletionDialog)
                },
            )
        }
    }
}
