package com.socialvideodownloader.feature.download.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.socialvideodownloader.feature.download.ui.DownloadScreen
import kotlinx.serialization.Serializable

@Serializable
object DownloadRoute

fun NavGraphBuilder.downloadScreen(isDarkTheme: Boolean, onToggleTheme: () -> Unit = {}) {
    composable<DownloadRoute> {
        DownloadScreen(isDarkTheme = isDarkTheme, onToggleTheme = onToggleTheme)
    }
}
