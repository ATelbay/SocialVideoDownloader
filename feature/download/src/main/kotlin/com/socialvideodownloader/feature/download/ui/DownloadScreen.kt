package com.socialvideodownloader.feature.download.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.socialvideodownloader.feature.download.navigation.PlatformLoginRoute
import com.socialvideodownloader.shared.feature.download.DownloadEvent
import com.socialvideodownloader.shared.feature.download.DownloadIntent
import com.socialvideodownloader.shared.feature.download.platform.PlatformActions
import com.socialvideodownloader.shared.network.auth.SupportedPlatform
import com.socialvideodownloader.shared.feature.download.ui.DownloadScreen as SharedDownloadScreen

@Composable
fun DownloadScreen(
    viewModel: DownloadViewModel = hiltViewModel(),
    initialUrl: String? = null,
    existingRecordId: Long? = null,
    navController: NavController = rememberNavController(),
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

    // Navigate to platform login screen when the shared VM emits ShowPlatformLogin
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event is DownloadEvent.ShowPlatformLogin) {
                navController.navigate(PlatformLoginRoute(event.platform.name))
            }
        }
    }

    // Observe platform login result passed back via savedStateHandle from PlatformLoginRoute
    val loginResult by navController.currentBackStackEntry
        ?.savedStateHandle
        ?.getStateFlow<String?>("platformLoginResult", null)
        ?.collectAsStateWithLifecycle(null)
        ?: remember { androidx.compose.runtime.mutableStateOf(null) }

    LaunchedEffect(loginResult) {
        val result = loginResult ?: return@LaunchedEffect
        navController.currentBackStackEntry?.savedStateHandle?.set<String?>("platformLoginResult", null)
        val parts = result.split(":")
        if (parts.size == 2) {
            val platformName = parts[0]
            val success = parts[1].toBooleanStrictOrNull() ?: false
            val platform = SupportedPlatform.entries.firstOrNull { it.name == platformName }
            if (platform != null) {
                viewModel.onIntent(DownloadIntent.PlatformLoginResult(platform, success))
            }
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
