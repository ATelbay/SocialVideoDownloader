package com.socialvideodownloader.feature.library.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.socialvideodownloader.feature.library.R
import com.socialvideodownloader.shared.feature.library.ui.LibraryScreen as SharedLibraryScreen
import com.socialvideodownloader.shared.feature.library.ui.LibraryStrings

@Composable
fun LibraryScreen(
    onNavigateToDownload: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
) {
    SharedLibraryScreen(
        viewModel = viewModel.shared,
        onNavigateToDownload = onNavigateToDownload,
        strings =
            LibraryStrings(
                screenTitle = stringResource(R.string.library_screen_title),
                emptyTitle = stringResource(R.string.library_empty_title),
                emptyDescription = stringResource(R.string.library_empty_description),
                startDownloading = stringResource(R.string.library_start_downloading),
                openError = stringResource(R.string.library_open_error),
                shareError = stringResource(R.string.library_share_error),
                deleted = stringResource(R.string.library_deleted),
            ),
        modifier = modifier,
    )
}
