package com.socialvideodownloader.feature.history.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.socialvideodownloader.feature.history.ui.HistoryScreen
import kotlinx.serialization.Serializable

@Serializable
object HistoryRoute

fun NavGraphBuilder.historyScreen() {
    composable<HistoryRoute> {
        HistoryScreen()
    }
}
