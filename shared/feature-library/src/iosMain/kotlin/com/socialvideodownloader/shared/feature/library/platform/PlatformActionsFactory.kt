package com.socialvideodownloader.shared.feature.library.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberPlatformActions(): PlatformActions = remember { PlatformActions() }
