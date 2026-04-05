package com.socialvideodownloader.shared.feature.download.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.socialvideodownloader.shared.network.auth.SecureCookieStore
import com.socialvideodownloader.shared.network.auth.SupportedPlatform

@Composable
actual fun PlatformLoginScreen(
    platform: SupportedPlatform,
    secureCookieStore: SecureCookieStore,
    onResult: (Boolean) -> Unit,
    modifier: Modifier,
) {
    // Android uses navigation-based PlatformLoginScreen in :feature:download module.
    // This shared actual is a no-op — login is handled by PlatformDelegate.showPlatformLogin().
    error("Android login is handled via PlatformDelegate navigation, not shared composable")
}
