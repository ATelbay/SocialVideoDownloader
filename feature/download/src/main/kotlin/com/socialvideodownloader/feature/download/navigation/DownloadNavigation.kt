package com.socialvideodownloader.feature.download.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.socialvideodownloader.feature.download.ui.DownloadScreen
import com.socialvideodownloader.feature.download.ui.PlatformLoginScreen
import com.socialvideodownloader.shared.network.auth.SecureCookieStore
import com.socialvideodownloader.shared.network.auth.SupportedPlatform
import kotlinx.serialization.Serializable
import org.koin.mp.KoinPlatform

@Serializable
data class DownloadRoute(
    val initialUrl: String? = null,
    val existingRecordId: Long? = null,
)

@Serializable
data class PlatformLoginRoute(val platformName: String)

fun NavGraphBuilder.downloadScreen(navController: NavController) {
    composable<DownloadRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<DownloadRoute>()
        // The platform-login destination writes its result back into this entry's
        // savedStateHandle (see PlatformLoginRoute below). Pass the handle access as
        // a flow + consume lambda so DownloadScreen stays NavController-free.
        val savedStateHandle = backStackEntry.savedStateHandle
        DownloadScreen(
            initialUrl = route.initialUrl,
            existingRecordId = route.existingRecordId,
            onNavigateToPlatformLogin = { platform ->
                navController.navigate(PlatformLoginRoute(platform.name))
            },
            platformLoginResultFlow =
                savedStateHandle.getStateFlow<String?>("platformLoginResult", null),
            onConsumePlatformLoginResult = {
                savedStateHandle["platformLoginResult"] = null
            },
        )
    }

    composable<PlatformLoginRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<PlatformLoginRoute>()
        val platform = SupportedPlatform.entries.firstOrNull { it.name == route.platformName }
        if (platform != null) {
            val secureCookieStore = KoinPlatform.getKoin().get<SecureCookieStore>()
            PlatformLoginScreen(
                platform = platform,
                secureCookieStore = secureCookieStore,
                onResult = { success ->
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("platformLoginResult", "${route.platformName}:$success")
                    navController.popBackStack()
                },
            )
        }
    }
}
