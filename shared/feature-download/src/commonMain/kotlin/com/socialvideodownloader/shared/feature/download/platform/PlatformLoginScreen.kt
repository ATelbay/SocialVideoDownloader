package com.socialvideodownloader.shared.feature.download.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.socialvideodownloader.shared.network.auth.CookieStore
import com.socialvideodownloader.shared.network.auth.SupportedPlatform

@Composable
expect fun PlatformLoginScreen(
    platform: SupportedPlatform,
    secureCookieStore: CookieStore,
    onResult: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
)
