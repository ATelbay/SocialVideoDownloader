package com.socialvideodownloader.feature.download.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.socialvideodownloader.shared.feature.download.DownloadIntent
import com.socialvideodownloader.shared.feature.download.platform.PlatformActions
import com.socialvideodownloader.shared.feature.download.ui.DownloadScreen as SharedDownloadScreen

@Composable
fun DownloadScreen(
    viewModel: DownloadViewModel = hiltViewModel(),
    initialUrl: String? = null,
    existingRecordId: Long? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { granted ->
            viewModel.onNotificationPermissionResult(granted)
        }

    LaunchedEffect(initialUrl) {
        if (initialUrl != null) {
            viewModel.onIntent(DownloadIntent.PrefillUrl(initialUrl, existingRecordId))
        }
    }

    val platformActions =
        remember(context) {
            PlatformActions(context) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

    SharedDownloadScreen(
        viewModel = viewModel.shared,
        platformActions = platformActions,
        modifier = modifier,
    )
}
