package com.socialvideodownloader.feature.library.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.socialvideodownloader.shared.feature.library.ui.rememberLibraryStrings
import com.socialvideodownloader.shared.feature.library.ui.LibraryScreen as SharedLibraryScreen

@Composable
fun LibraryScreen(
    onNavigateToDownload: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
) {
    SharedLibraryScreen(
        viewModel = viewModel.shared,
        onNavigateToDownload = onNavigateToDownload,
        strings = rememberLibraryStrings(),
        modifier = modifier,
    )
}
