package com.socialvideodownloader.feature.library.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.socialvideodownloader.feature.library.ui.LibraryScreen
import kotlinx.serialization.Serializable

@Serializable
data object LibraryRoute

fun NavGraphBuilder.libraryScreen(onNavigateToDownload: () -> Unit) {
    composable<LibraryRoute> {
        LibraryScreen(onNavigateToDownload = onNavigateToDownload)
    }
}
