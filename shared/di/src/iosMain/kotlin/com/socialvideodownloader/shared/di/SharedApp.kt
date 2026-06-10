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
import com.socialvideodownloader.shared.di.generated.resources.Res
import com.socialvideodownloader.shared.di.generated.resources.nav_tab_download
import com.socialvideodownloader.shared.di.generated.resources.nav_tab_history
import com.socialvideodownloader.shared.di.generated.resources.nav_tab_library
import com.socialvideodownloader.shared.di.generated.resources.size_unit_b
import com.socialvideodownloader.shared.di.generated.resources.size_unit_gb
import com.socialvideodownloader.shared.di.generated.resources.size_unit_kb
import com.socialvideodownloader.shared.di.generated.resources.size_unit_mb
import com.socialvideodownloader.shared.feature.download.DownloadIntent
import com.socialvideodownloader.shared.feature.download.SharedDownloadViewModel
import com.socialvideodownloader.shared.feature.download.ui.DownloadScreen
import com.socialvideodownloader.shared.feature.history.SharedHistoryViewModel
import com.socialvideodownloader.shared.feature.history.platform.openUpgradeFlow
import com.socialvideodownloader.shared.feature.history.platform.shareFile
import com.socialvideodownloader.shared.feature.history.platform.triggerGoogleSignIn
import com.socialvideodownloader.shared.feature.history.ui.GoogleSignInResult
import com.socialvideodownloader.shared.feature.history.ui.HistoryScreen
import com.socialvideodownloader.shared.feature.history.ui.formatTimestamp
import com.socialvideodownloader.shared.feature.history.ui.rememberHistoryStrings
import com.socialvideodownloader.shared.feature.library.SharedLibraryViewModel
import com.socialvideodownloader.shared.feature.library.ui.LibraryScreen
import com.socialvideodownloader.shared.feature.library.ui.rememberLibraryStrings
import com.socialvideodownloader.shared.ui.components.PillNavItem
import com.socialvideodownloader.shared.ui.components.PillNavigationBar
import com.socialvideodownloader.shared.ui.components.SvdNavIcons
import com.socialvideodownloader.shared.ui.theme.SvdBg
import com.socialvideodownloader.shared.ui.theme.SvdTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.jetbrains.compose.resources.stringResource
import org.koin.core.parameter.parametersOf
import org.koin.mp.KoinPlatform
import platform.Foundation.NSLog
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

        val sizeUnitB = stringResource(Res.string.size_unit_b)
        val sizeUnitKb = stringResource(Res.string.size_unit_kb)
        val sizeUnitMb = stringResource(Res.string.size_unit_mb)
        val sizeUnitGb = stringResource(Res.string.size_unit_gb)

        val navItems =
            listOf(
                PillNavItem(stringResource(Res.string.nav_tab_download), SvdNavIcons.Download),
                PillNavItem(stringResource(Res.string.nav_tab_library), SvdNavIcons.Library),
                PillNavItem(stringResource(Res.string.nav_tab_history), SvdNavIcons.History),
            )

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
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            strings = rememberLibraryStrings(),
                        )
                    }
                    composable(DEST_HISTORY) {
                        HistoryScreen(
                            viewModel = historyVm,
                            strings = rememberHistoryStrings(),
                            formattedDate = { epochMillis ->
                                formatEpochMillis(epochMillis)
                            },
                            formattedSize = { bytes ->
                                formatBytes(bytes, sizeUnitB, sizeUnitKb, sizeUnitMb, sizeUnitGb)
                            },
                            onNavigateToDownload = { initialUrl, _ ->
                                selectedTab = 0
                                navController.navigate(DEST_DOWNLOAD) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
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
                                triggerGoogleSignIn().fold(
                                    onSuccess = { GoogleSignInResult.Success(it) },
                                    onFailure = {
                                        NSLog("Google sign-in failed: ${it.message}")
                                        GoogleSignInResult.Failed
                                    },
                                )
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
                    items = navItems,
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

private fun formatBytes(
    bytes: Long,
    unitB: String,
    unitKb: String,
    unitMb: String,
    unitGb: String,
): String {
    if (bytes < 1024) return "$bytes $unitB"
    val kb = bytes / 1024.0
    if (kb < 1024) return "${kb.toInt()} $unitKb"
    val mb = kb / 1024.0
    if (mb < 1024) return "${(mb * 10).toInt() / 10.0} $unitMb"
    val gb = mb / 1024.0
    return "${(gb * 100).toInt() / 100.0} $unitGb"
}

private fun formatEpochMillis(epochMillis: Long): String = formatTimestamp(epochMillis)
