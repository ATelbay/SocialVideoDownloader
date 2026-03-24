package com.socialvideodownloader.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.socialvideodownloader.feature.download.navigation.DownloadRoute
import com.socialvideodownloader.feature.download.navigation.downloadScreen
import com.socialvideodownloader.feature.history.navigation.historyScreen
import com.socialvideodownloader.feature.library.navigation.libraryScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: DownloadRoute = DownloadRoute(),
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        downloadScreen()
        libraryScreen(
            onNavigateToDownload = {
                navController.navigate(DownloadRoute()) {
                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
        )
        historyScreen(
            onNavigateToDownload = { initialUrl, existingRecordId ->
                navController.navigate(DownloadRoute(initialUrl = initialUrl, existingRecordId = existingRecordId)) {
                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                    launchSingleTop = true
                    restoreState = false
                }
            },
        )
    }
}
