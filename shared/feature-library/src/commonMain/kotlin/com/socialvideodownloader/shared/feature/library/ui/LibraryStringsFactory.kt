package com.socialvideodownloader.shared.feature.library.ui

import androidx.compose.runtime.Composable
import com.socialvideodownloader.shared.feature.library.generated.resources.Res
import com.socialvideodownloader.shared.feature.library.generated.resources.library_deleted
import com.socialvideodownloader.shared.feature.library.generated.resources.library_empty_description
import com.socialvideodownloader.shared.feature.library.generated.resources.library_empty_title
import com.socialvideodownloader.shared.feature.library.generated.resources.library_open_error
import com.socialvideodownloader.shared.feature.library.generated.resources.library_screen_title
import com.socialvideodownloader.shared.feature.library.generated.resources.library_share_error
import com.socialvideodownloader.shared.feature.library.generated.resources.library_start_downloading
import org.jetbrains.compose.resources.stringResource

/**
 * Builds [LibraryStrings] from the shared Compose Multiplatform string resources so that
 * Android and the iOS shell read from a single source of truth.
 */
@Composable
fun rememberLibraryStrings(): LibraryStrings =
    LibraryStrings(
        screenTitle = stringResource(Res.string.library_screen_title),
        emptyTitle = stringResource(Res.string.library_empty_title),
        emptyDescription = stringResource(Res.string.library_empty_description),
        startDownloading = stringResource(Res.string.library_start_downloading),
        openError = stringResource(Res.string.library_open_error),
        shareError = stringResource(Res.string.library_share_error),
        deleted = stringResource(Res.string.library_deleted),
    )
