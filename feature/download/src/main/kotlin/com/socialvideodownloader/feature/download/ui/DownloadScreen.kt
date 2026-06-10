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
import com.socialvideodownloader.shared.feature.download.DownloadIntent
import com.socialvideodownloader.shared.feature.download.platform.PlatformActions
import com.socialvideodownloader.shared.network.auth.SupportedPlatform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.socialvideodownloader.shared.feature.download.ui.DownloadScreen as SharedDownloadScreen

@Composable
fun DownloadScreen(
    viewModel: DownloadViewModel = hiltViewModel(),
    initialUrl: String? = null,
    existingRecordId: Long? = null,
    onNavigateToPlatformLogin: (SupportedPlatform) -> Unit = {},
    platformLoginResultFlow: StateFlow<String?>? = null,
    onConsumePlatformLoginResult: () -> Unit = {},
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

    // Navigate to platform login screen via Android-only channel (not shared events).
    // Navigation is provided as a lambda so this screen stays decoupled from NavController.
    LaunchedEffect(Unit) {
        viewModel.platformLoginNav.collect { platform ->
            onNavigateToPlatformLogin(platform)
        }
    }

    // Clear any stale platform-login result on first composition to avoid spurious
    // re-extraction after process death.
    LaunchedEffect(Unit) {
        onConsumePlatformLoginResult()
    }

    val resultFlow = platformLoginResultFlow ?: remember { MutableStateFlow(null) }
    val loginResult by resultFlow.collectAsStateWithLifecycle(null)

    LaunchedEffect(loginResult) {
        val result = loginResult ?: return@LaunchedEffect
        onConsumePlatformLoginResult()
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
