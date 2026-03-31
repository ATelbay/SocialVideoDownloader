package com.socialvideodownloader.shared.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

@Composable
expect fun resolveColorScheme(dynamicColor: Boolean): ColorScheme
