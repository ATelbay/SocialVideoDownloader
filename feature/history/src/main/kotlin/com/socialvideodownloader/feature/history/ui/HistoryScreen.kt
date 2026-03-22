package com.socialvideodownloader.feature.history.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.socialvideodownloader.core.domain.model.DownloadStatus
import com.socialvideodownloader.core.ui.components.SvdTopBar
import com.socialvideodownloader.core.ui.theme.AppShapesInstance
import com.socialvideodownloader.core.ui.theme.SvdBg
import com.socialvideodownloader.core.ui.theme.SvdBorder
import com.socialvideodownloader.core.ui.theme.SvdForeground
import com.socialvideodownloader.core.ui.theme.SvdSubtleForeground
import com.socialvideodownloader.core.ui.theme.SvdSurface
import com.socialvideodownloader.core.ui.tokens.Spacing
import com.socialvideodownloader.feature.history.R
import com.socialvideodownloader.feature.history.components.HistoryBottomSheet
import com.socialvideodownloader.feature.history.components.HistoryDeleteDialog
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onNavigateToDownload: (initialUrl: String) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    // US3: Billing — controls visibility of upgrade dialog
    var showUpgradeDialog by rememberSaveable { mutableStateOf(false) }

    // Credential Manager for Google Sign-In
    val credentialManager = remember { CredentialManager.create(context) }

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
                        snackbarHostState.showSnackbar(
                            context.getString(R.string.history_open_error),
                        )
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
                        snackbarHostState.showSnackbar(
                            context.getString(R.string.history_share_error),
                        )
                    }
                }
                is HistoryEffect.RetryDownload -> {
                    onNavigateToDownload(effect.sourceUrl)
                }
                // US3: Billing — show upgrade dialog
                is HistoryEffect.LaunchUpgradeFlow -> {
                    showUpgradeDialog = true
                }
                // Google Sign-In — launch Credential Manager
                is HistoryEffect.LaunchGoogleSignIn -> {
                    val activity = context as? Activity
                    if (activity == null) {
                        Log.e("HistoryScreen", "No Activity context for Credential Manager")
                        return@collect
                    }
                    coroutineScope.launch {
                        try {
                            val googleIdOption = GetGoogleIdOption.Builder()
                                .setFilterByAuthorizedAccounts(false)
                                .setServerClientId(
                                    context.getString(R.string.google_web_client_id),
                                )
                                .build()
                            val request = GetCredentialRequest.Builder()
                                .addCredentialOption(googleIdOption)
                                .build()
                            val result = credentialManager.getCredential(
                                context = activity,
                                request = request,
                            )
                            val googleIdTokenCredential =
                                GoogleIdTokenCredential.createFrom(result.credential.data)
                            viewModel.onIntent(
                                HistoryIntent.SignInWithGoogle(googleIdTokenCredential.idToken),
                            )
                        } catch (e: GetCredentialCancellationException) {
                            // User cancelled — no-op
                        } catch (e: Exception) {
                            Log.e("HistoryScreen", "Google sign-in failed", e)
                            snackbarHostState.showSnackbar(
                                context.getString(R.string.cloud_sign_in_failed),
                            )
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = SvdBg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column {
                if (isSearchActive) {
                    // Search mode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(Spacing.TopBarHeight)
                            .padding(horizontal = Spacing.TopBarPaddingH),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { query ->
                                searchQuery = query
                                viewModel.onIntent(HistoryIntent.SearchQueryChanged(query))
                            },
                            placeholder = {
                                Text(
                                    text = stringResource(R.string.history_search_hint),
                                    color = SvdSubtleForeground,
                                )
                            },
                            singleLine = true,
                            shape = AppShapesInstance.control,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = SvdSurface,
                                unfocusedContainerColor = SvdSurface,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent,
                                focusedTextColor = SvdForeground,
                                unfocusedTextColor = SvdForeground,
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, SvdBorder, AppShapesInstance.control),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(AppShapesInstance.control)
                                .background(SvdSurface),
                            contentAlignment = Alignment.Center,
                        ) {
                            androidx.compose.material3.IconButton(
                                onClick = {
                                    searchQuery = ""
                                    isSearchActive = false
                                    viewModel.onIntent(HistoryIntent.SearchQueryChanged(""))
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    tint = SvdForeground,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                } else {
                    SvdTopBar(
                        title = stringResource(R.string.history_screen_title_full),
                        actionLabel = stringResource(R.string.history_action_filter),
                        onActionClick = { isSearchActive = true },
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        // US3: Billing — show capacity banner when near limit
        val contentState = uiState as? HistoryUiState.Content
        // US1: Cloud backup state
        val cloudBackupState by viewModel.cloudBackupState.collectAsStateWithLifecycle()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            CloudBackupSection(
                state = cloudBackupState,
                onToggleBackup = { viewModel.onIntent(HistoryIntent.ToggleCloudBackup) },
                onSignOut = { viewModel.onIntent(HistoryIntent.SignOutCloud) },
            )
            if (cloudBackupState.isCloudBackupEnabled) {
                TextButton(
                    onClick = { viewModel.onIntent(HistoryIntent.RestoreFromCloud) },
                    modifier = Modifier.padding(horizontal = 8.dp),
                ) {
                    Text(text = stringResource(R.string.cloud_restore_button))
                }
            }
            // US2: Restore dialog
            if (cloudBackupState.restoreState != RestoreState.Idle) {
                RestoreDialog(
                    restoreState = cloudBackupState.restoreState,
                    onDismiss = { viewModel.onIntent(HistoryIntent.DismissRestoreDialog) },
                )
            }
            if (contentState?.cloudCapacity?.isNearLimit == true) {
                CapacityBanner(
                    capacity = contentState.cloudCapacity,
                    onUpgradeClick = { viewModel.onIntent(HistoryIntent.TapUpgrade) },
                )
            }
            HistoryContent(
                uiState = uiState,
                onIntent = viewModel::onIntent,
                onStartDownloading = { onNavigateToDownload("") },
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Re-declare for bottom sheet / dialog use below
        val openItemId = contentState?.openMenuItemId
        if (openItemId != null) {
            val selectedItem = contentState.items.find { it.id == openItemId }
            if (selectedItem != null) {
                HistoryBottomSheet(
                    title = selectedItem.title,
                    showShare = selectedItem.status != DownloadStatus.FAILED,
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

        // US3: Billing — upgrade dialog triggered by CapacityBanner or effect
        if (showUpgradeDialog) {
            UpgradeScreen(
                onBuyClick = {
                    showUpgradeDialog = false
                    val activity = context as? Activity
                    if (activity != null) {
                        coroutineScope.launch {
                            viewModel.launchPurchaseFlow(activity)
                        }
                    }
                },
                onDismiss = { showUpgradeDialog = false },
            )
        }
    }
}
