package com.socialvideodownloader.shared.feature.history.ui

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
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.socialvideodownloader.core.domain.model.DownloadStatus
import com.socialvideodownloader.shared.feature.history.HistoryEffect
import com.socialvideodownloader.shared.feature.history.HistoryIntent
import com.socialvideodownloader.shared.feature.history.HistoryMessageType
import com.socialvideodownloader.shared.feature.history.HistoryUiState
import com.socialvideodownloader.shared.feature.history.RestoreState
import com.socialvideodownloader.shared.feature.history.SharedHistoryViewModel
import com.socialvideodownloader.shared.ui.components.SvdTopBar
import com.socialvideodownloader.shared.ui.theme.LocalAppShapes
import com.socialvideodownloader.shared.ui.theme.Spacing
import com.socialvideodownloader.shared.ui.theme.SvdBg
import com.socialvideodownloader.shared.ui.theme.SvdBorder
import com.socialvideodownloader.shared.ui.theme.SvdForeground
import com.socialvideodownloader.shared.ui.theme.SvdSubtleForeground
import com.socialvideodownloader.shared.ui.theme.SvdSurface

data class HistoryStrings(
    val screenTitle: String,
    val filterActionLabel: String,
    val searchHint: String,
    val emptyTitle: String,
    val emptyDescription: String,
    val noResultsDescription: String,
    val startDownloadingLabel: String,
    val startNewDownloadLabel: String,
    val restoreButtonLabel: String,
    val capacityBannerText: (used: Int, limit: Int) -> String,
    val capacityUpgradeLabel: String,
    val okLabel: String,
    val restoreProgressText: (current: Int, total: Int) -> String,
    val restoreCompletedText: (restored: Int, skipped: Int) -> String,
    val restoreKeyLostText: String,
    val deleteTitle: String,
    val deleteBodyText: String,
    val deleteFilesLabel: String,
    val deleteCancelLabel: String,
    val deleteConfirmLabel: String,
    val bottomSheetCopyLinkLabel: String,
    val bottomSheetShareLabel: String,
    val bottomSheetDeleteLabel: String,
    val upgradeTitle: String,
    val upgradeDescription: String,
    val upgradePriceLabel: String,
    val upgradeBuyLabel: String,
    val upgradeCancelLabel: String,
    val cloudBackupToggleLabel: String,
    val cloudSignInLabel: String,
    val cloudSignOutLabel: String,
    val cloudSignedInAs: String,
    val cloudSignInFailedMessage: String,
    val cloudBackupDisabledText: String,
    val cloudBackupNeverText: String,
    val cloudBackupSyncingText: String,
    val cloudBackupSyncedText: (time: String) -> String,
    val cloudBackupPausedText: String,
    val cloudBackupErrorText: String,
    val msgDeleted: String,
    val msgAllDeleted: String,
    val msgLinkCopied: String,
    val msgCloudSyncError: String,
    val msgFileUnavailable: String,
    val msgDeleteFileFailed: String,
    val msgOpenError: String,
    val msgShareError: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: SharedHistoryViewModel,
    strings: HistoryStrings,
    formattedDate: (epochMillis: Long) -> String,
    formattedSize: (bytes: Long) -> String,
    onNavigateToDownload: (initialUrl: String, existingRecordId: Long?) -> Unit,
    onOpenFile: (uri: String) -> Unit,
    onShareFile: (uri: String) -> Unit,
    onLaunchGoogleSignIn: suspend () -> String?,
    onLaunchUpgradeFlow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val cloudBackupState by viewModel.cloudBackupState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    var showUpgradeDialog by rememberSaveable { mutableStateOf(false) }
    val shapes = LocalAppShapes.current

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            snackbarHostState.currentSnackbarData?.dismiss()
            when (effect) {
                is HistoryEffect.ShowMessage -> {
                    val msg =
                        when (effect.messageType) {
                            HistoryMessageType.DELETE_SUCCESS -> strings.msgDeleted
                            HistoryMessageType.DELETE_ALL_SUCCESS -> strings.msgAllDeleted
                            HistoryMessageType.COPY_URL_SUCCESS -> strings.msgLinkCopied
                            HistoryMessageType.CLOUD_SYNC_ERROR -> strings.msgCloudSyncError
                            HistoryMessageType.FILE_UNAVAILABLE -> strings.msgFileUnavailable
                            HistoryMessageType.DELETE_FILE_FAILED -> strings.msgDeleteFileFailed
                        }
                    snackbarHostState.showSnackbar(msg)
                }
                is HistoryEffect.OpenContent -> {
                    try {
                        onOpenFile(effect.contentUri)
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar(strings.msgOpenError)
                    }
                }
                is HistoryEffect.ShareContent -> {
                    try {
                        onShareFile(effect.contentUri)
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar(strings.msgShareError)
                    }
                }
                is HistoryEffect.RetryDownload -> {
                    onNavigateToDownload(effect.sourceUrl, effect.existingRecordId)
                }
                is HistoryEffect.LaunchUpgradeFlow -> {
                    showUpgradeDialog = true
                }
                is HistoryEffect.LaunchGoogleSignIn -> {
                    val idToken = onLaunchGoogleSignIn()
                    if (idToken != null) {
                        viewModel.onIntent(HistoryIntent.SignInWithGoogle(idToken))
                    } else {
                        viewModel.onIntent(HistoryIntent.SignInCancelled)
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
                    Row(
                        modifier =
                            Modifier
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
                                    text = strings.searchHint,
                                    color = SvdSubtleForeground,
                                )
                            },
                            singleLine = true,
                            shape = shapes.control,
                            colors =
                                TextFieldDefaults.colors(
                                    focusedContainerColor = SvdSurface,
                                    unfocusedContainerColor = SvdSurface,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    disabledIndicatorColor = Color.Transparent,
                                    focusedTextColor = SvdForeground,
                                    unfocusedTextColor = SvdForeground,
                                ),
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .border(1.dp, SvdBorder, shapes.control)
                                    .semantics {
                                        contentDescription = strings.searchHint
                                    },
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier =
                                Modifier
                                    .size(36.dp)
                                    .clip(shapes.control)
                                    .background(SvdSurface),
                            contentAlignment = Alignment.Center,
                        ) {
                            IconButton(
                                onClick = {
                                    searchQuery = ""
                                    isSearchActive = false
                                    viewModel.onIntent(HistoryIntent.SearchQueryChanged(""))
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear search",
                                    tint = SvdForeground,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                } else {
                    SvdTopBar(
                        title = strings.screenTitle,
                        actionLabel = strings.filterActionLabel,
                        onActionClick = { isSearchActive = true },
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        val contentState = uiState as? HistoryUiState.Content

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            CloudBackupSection(
                state = cloudBackupState,
                backupToggleLabel = strings.cloudBackupToggleLabel,
                signInLabel = strings.cloudSignInLabel,
                signOutLabel = strings.cloudSignOutLabel,
                signedInAs = strings.cloudSignedInAs,
                signInFailedMessage = strings.cloudSignInFailedMessage,
                backupDisabledText = strings.cloudBackupDisabledText,
                backupNeverText = strings.cloudBackupNeverText,
                backupSyncingText = strings.cloudBackupSyncingText,
                backupSyncedText = strings.cloudBackupSyncedText,
                backupPausedText = strings.cloudBackupPausedText,
                backupErrorText = strings.cloudBackupErrorText,
                onToggleBackup = { viewModel.onIntent(HistoryIntent.ToggleCloudBackup) },
                onSignOut = { viewModel.onIntent(HistoryIntent.SignOutCloud) },
            )

            if (cloudBackupState.isCloudBackupEnabled) {
                TextButton(
                    onClick = { viewModel.onIntent(HistoryIntent.RestoreFromCloud) },
                    modifier = Modifier.padding(horizontal = 8.dp),
                ) {
                    Text(text = strings.restoreButtonLabel)
                }
            }

            if (cloudBackupState.restoreState != RestoreState.Idle) {
                RestoreDialog(
                    restoreState = cloudBackupState.restoreState,
                    okLabel = strings.okLabel,
                    progressText = strings.restoreProgressText,
                    completedText = strings.restoreCompletedText,
                    keyLostText = strings.restoreKeyLostText,
                    onDismiss = { viewModel.onIntent(HistoryIntent.DismissRestoreDialog) },
                )
            }

            val capacity = contentState?.cloudCapacity
            if (capacity?.isNearLimit == true) {
                CapacityBanner(
                    capacity = capacity,
                    bannerText = strings.capacityBannerText(capacity.used, capacity.limit),
                    upgradeLabel = strings.capacityUpgradeLabel,
                    onUpgradeClick = { viewModel.onIntent(HistoryIntent.TapUpgrade) },
                )
            }

            HistoryContent(
                uiState = uiState,
                emptyTitle = strings.emptyTitle,
                emptyDescription = strings.emptyDescription,
                noResultsDescription = strings.noResultsDescription,
                startDownloadingLabel = strings.startDownloadingLabel,
                startNewDownloadLabel = strings.startNewDownloadLabel,
                formattedDate = formattedDate,
                formattedSize = formattedSize,
                onIntent = viewModel::onIntent,
                onStartDownloading = { onNavigateToDownload("", null) },
                modifier = Modifier.fillMaxSize(),
            )
        }

        val openItemId = contentState?.openMenuItemId
        if (openItemId != null) {
            val selectedItem = contentState.items.find { it.id == openItemId }
            if (selectedItem != null) {
                HistoryBottomSheet(
                    title = selectedItem.title,
                    showShare = selectedItem.status == DownloadStatus.COMPLETED && selectedItem.isFileAccessible,
                    copyLinkLabel = strings.bottomSheetCopyLinkLabel,
                    shareLabel = strings.bottomSheetShareLabel,
                    deleteLabel = strings.bottomSheetDeleteLabel,
                    onCopyLink = {
                        viewModel.onIntent(HistoryIntent.CopyLinkClicked(openItemId))
                        viewModel.onIntent(HistoryIntent.DismissItemMenu)
                    },
                    onShare = {
                        viewModel.onIntent(HistoryIntent.ShareClicked(openItemId))
                        viewModel.onIntent(HistoryIntent.DismissItemMenu)
                    },
                    onDelete = {
                        viewModel.onIntent(HistoryIntent.DeleteItemClicked(openItemId))
                        viewModel.onIntent(HistoryIntent.DismissItemMenu)
                    },
                    onDismiss = { viewModel.onIntent(HistoryIntent.DismissItemMenu) },
                )
            }
        }

        contentState?.deleteConfirmation?.let { confirmation ->
            HistoryDeleteDialog(
                state = confirmation,
                title = strings.deleteTitle,
                bodyText = strings.deleteBodyText,
                deleteFilesLabel = strings.deleteFilesLabel,
                cancelLabel = strings.deleteCancelLabel,
                confirmLabel = strings.deleteConfirmLabel,
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

        if (showUpgradeDialog) {
            UpgradeScreen(
                title = strings.upgradeTitle,
                description = strings.upgradeDescription,
                priceLabel = strings.upgradePriceLabel,
                buyLabel = strings.upgradeBuyLabel,
                cancelLabel = strings.upgradeCancelLabel,
                onBuyClick = {
                    showUpgradeDialog = false
                    onLaunchUpgradeFlow()
                },
                onDismiss = { showUpgradeDialog = false },
            )
        }
    }
}
