package com.socialvideodownloader.shared.di

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.socialvideodownloader.shared.feature.download.DownloadIntent
import com.socialvideodownloader.shared.feature.download.SharedDownloadViewModel
import com.socialvideodownloader.shared.feature.download.ui.DownloadScreen
import com.socialvideodownloader.shared.feature.history.SharedHistoryViewModel
import com.socialvideodownloader.shared.feature.history.platform.openUpgradeFlow
import com.socialvideodownloader.shared.feature.history.platform.shareFile
import com.socialvideodownloader.shared.feature.history.platform.triggerGoogleSignIn
import com.socialvideodownloader.shared.feature.history.ui.HistoryScreen
import com.socialvideodownloader.shared.feature.history.ui.HistoryStrings
import com.socialvideodownloader.shared.feature.history.ui.formatTimestamp
import com.socialvideodownloader.shared.feature.library.SharedLibraryViewModel
import com.socialvideodownloader.shared.feature.library.ui.LibraryScreen
import com.socialvideodownloader.shared.feature.library.ui.LibraryStrings
import com.socialvideodownloader.shared.ui.components.PillNavigationBar
import com.socialvideodownloader.shared.ui.theme.SvdBg
import com.socialvideodownloader.shared.ui.theme.SvdTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.parameter.parametersOf
import org.koin.mp.KoinPlatform
import com.socialvideodownloader.shared.feature.download.platform.PlatformActions as DownloadPlatformActions

private const val DEST_DOWNLOAD = "download"
private const val DEST_LIBRARY = "library"
private const val DEST_HISTORY = "history"

private val tabDestinations = listOf(DEST_DOWNLOAD, DEST_LIBRARY, DEST_HISTORY)

@Composable
fun SharedApp() {
    SvdTheme {
        val navController = rememberNavController()
        var selectedTab by rememberSaveable { mutableStateOf(0) }

        val downloadVm =
            remember {
                val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
                KoinPlatform.getKoin().get<SharedDownloadViewModel> { parametersOf(scope) }
            }
        val libraryVm =
            remember {
                val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
                KoinPlatform.getKoin().get<SharedLibraryViewModel> { parametersOf(scope) }
            }
        val historyVm =
            remember {
                val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
                KoinPlatform.getKoin().get<SharedHistoryViewModel> { parametersOf(scope) }
            }

        DisposableEffect(Unit) {
            onDispose {
                downloadVm.cleanup()
                libraryVm.cleanup()
                historyVm.cleanup()
            }
        }

        val downloadPlatformActions = remember { DownloadPlatformActions() }

        // On app foreground, pick up any URL written by the Share Extension or
        // the socialvideodownloader:// URL scheme handler and prefill the download screen.
        LaunchedEffect(Unit) {
            val pendingUrl = downloadPlatformActions.getPendingSharedUrl()
            if (!pendingUrl.isNullOrEmpty()) {
                downloadPlatformActions.clearPendingSharedUrl()
                downloadVm.onIntent(DownloadIntent.PrefillUrl(pendingUrl))
            }
        }

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(SvdBg),
        ) {
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(SvdBg),
            ) {
                NavHost(
                    navController = navController,
                    startDestination = DEST_DOWNLOAD,
                ) {
                    composable(DEST_DOWNLOAD) {
                        DownloadScreen(
                            viewModel = downloadVm,
                            platformActions = downloadPlatformActions,
                        )
                    }
                    composable(DEST_LIBRARY) {
                        LibraryScreen(
                            viewModel = libraryVm,
                            onNavigateToDownload = {
                                selectedTab = 0
                                navController.navigate(DEST_DOWNLOAD) {
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            strings = defaultLibraryStrings(),
                        )
                    }
                    composable(DEST_HISTORY) {
                        HistoryScreen(
                            viewModel = historyVm,
                            strings = defaultHistoryStrings(),
                            formattedDate = { epochMillis ->
                                formatEpochMillis(epochMillis)
                            },
                            formattedSize = { bytes ->
                                formatBytes(bytes)
                            },
                            onNavigateToDownload = { initialUrl, _ ->
                                selectedTab = 0
                                navController.navigate(DEST_DOWNLOAD) {
                                    launchSingleTop = true
                                    restoreState = true
                                }
                                if (initialUrl.isNotEmpty()) {
                                    downloadVm.onIntent(DownloadIntent.PrefillUrl(initialUrl))
                                }
                            },
                            onOpenFile = { uri ->
                                downloadPlatformActions.openFile(uri)
                            },
                            onShareFile = { uri ->
                                shareFile(uri)
                            },
                            onLaunchGoogleSignIn = {
                                val result = triggerGoogleSignIn()
                                result.getOrNull()
                            },
                            onLaunchUpgradeFlow = {
                                openUpgradeFlow()
                            },
                        )
                    }
                }
            }

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(SvdBg),
            ) {
                PillNavigationBar(
                    selectedIndex = selectedTab,
                    onSelect = { index ->
                        selectedTab = index
                        val destination = tabDestinations[index]
                        navController.navigate(destination) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .windowInsetsBottomHeight(WindowInsets.safeDrawing)
                            .background(SvdBg),
                )
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "${kb.toInt()} KB"
    val mb = kb / 1024.0
    if (mb < 1024) return "${(mb * 10).toInt() / 10.0} MB"
    val gb = mb / 1024.0
    return "${(gb * 100).toInt() / 100.0} GB"
}

private fun formatEpochMillis(epochMillis: Long): String = formatTimestamp(epochMillis)

private fun defaultLibraryStrings() =
    LibraryStrings(
        screenTitle = "Library",
        emptyTitle = "No downloads yet",
        emptyDescription = "Videos you download will appear here",
        startDownloading = "Start downloading",
        openError = "Could not open file",
        shareError = "Could not share file",
        deleted = "File deleted",
    )

private fun defaultHistoryStrings() =
    HistoryStrings(
        screenTitle = "History",
        filterActionLabel = "Search",
        searchHint = "Search downloads…",
        emptyTitle = "No history yet",
        emptyDescription = "Your download history will appear here",
        noResultsDescription = "No results for your search",
        startDownloadingLabel = "Start downloading",
        startNewDownloadLabel = "New download",
        restoreButtonLabel = "Restore from cloud",
        capacityBannerText = { used, limit -> "$used of $limit backups used" },
        capacityUpgradeLabel = "Upgrade",
        okLabel = "OK",
        restoreProgressText = { current, total -> "Restoring $current of $total…" },
        restoreCompletedText = { restored, skipped -> "Restored $restored, skipped $skipped" },
        restoreKeyLostText = "Encryption key not found. Restore unavailable.",
        deleteTitle = "Delete download",
        deleteBodyText = "Are you sure you want to delete this download?",
        deleteFilesLabel = "Also delete file from device",
        deleteCancelLabel = "Cancel",
        deleteConfirmLabel = "Delete",
        bottomSheetCopyLinkLabel = "Copy link",
        bottomSheetShareLabel = "Share",
        bottomSheetDeleteLabel = "Delete",
        upgradeTitle = "Upgrade to Pro",
        upgradeDescription = "Unlock unlimited cloud backup for all your downloads",
        upgradePriceLabel = "from \$1.99 / month",
        upgradeBuyLabel = "Upgrade",
        upgradeCancelLabel = "Not now",
        cloudBackupToggleLabel = "Cloud backup",
        cloudSignInLabel = "Sign in with Google",
        cloudSignOutLabel = "Sign out",
        cloudSignedInAs = "Signed in as",
        cloudSignInFailedMessage = "Sign-in failed. Please try again.",
        cloudBackupDisabledText = "Backup disabled",
        cloudBackupNeverText = "Never synced",
        cloudBackupSyncingText = "Syncing…",
        cloudBackupSyncedText = { time -> "Last synced $time" },
        cloudBackupPausedText = "Backup paused",
        cloudBackupErrorText = "Backup error",
        msgDeleted = "Download deleted",
        msgAllDeleted = "All downloads deleted",
        msgLinkCopied = "Link copied",
        msgCloudSyncError = "Cloud sync error",
        msgFileUnavailable = "File unavailable",
        msgDeleteFileFailed = "Could not delete file",
        msgOpenError = "Could not open file",
        msgShareError = "Could not share file",
    )
