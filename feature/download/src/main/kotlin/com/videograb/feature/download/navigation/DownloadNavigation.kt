package com.videograb.feature.download.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.videograb.feature.download.ui.DownloadScreen
import kotlinx.serialization.Serializable

@Serializable
object DownloadRoute

fun NavGraphBuilder.downloadScreen() {
    composable<DownloadRoute> {
        DownloadScreen()
    }
}
