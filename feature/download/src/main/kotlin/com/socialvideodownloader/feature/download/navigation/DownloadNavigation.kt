package com.socialvideodownloader.feature.download.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.socialvideodownloader.feature.download.ui.DownloadScreen
import kotlinx.serialization.Serializable

@Serializable
data class DownloadRoute(val initialUrl: String? = null)

fun NavGraphBuilder.downloadScreen() {
    composable<DownloadRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<DownloadRoute>()
        DownloadScreen(
            initialUrl = route.initialUrl,
        )
    }
}
