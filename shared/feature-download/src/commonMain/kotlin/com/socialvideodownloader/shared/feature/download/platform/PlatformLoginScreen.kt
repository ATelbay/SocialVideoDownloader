package com.socialvideodownloader.shared.feature.download.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.socialvideodownloader.shared.network.auth.SecureCookieStore
import com.socialvideodownloader.shared.network.auth.SupportedPlatform

@Composable
expect fun PlatformLoginScreen(
    platform: SupportedPlatform,
    secureCookieStore: SecureCookieStore,
    onResult: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
)
