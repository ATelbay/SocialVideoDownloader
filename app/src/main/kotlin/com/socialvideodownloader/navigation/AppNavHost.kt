package com.socialvideodownloader.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.socialvideodownloader.feature.download.navigation.DownloadRoute
import com.socialvideodownloader.feature.download.navigation.downloadScreen
import com.socialvideodownloader.feature.history.navigation.historyScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier,
    onToggleTheme: () -> Unit = {},
) {
    NavHost(
        navController = navController,
        startDestination = DownloadRoute(),
        modifier = modifier,
    ) {
        downloadScreen(isDarkTheme = isDarkTheme, onToggleTheme = onToggleTheme)
        historyScreen(
            onNavigateToDownload = { initialUrl ->
                navController.navigate(DownloadRoute(initialUrl = initialUrl)) {
                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                    launchSingleTop = true
                    restoreState = false
                }
            },
        )
    }
}
