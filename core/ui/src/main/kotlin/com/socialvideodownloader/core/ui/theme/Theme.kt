package com.socialvideodownloader.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

// Extension to access ExtendedColors from MaterialTheme
val MaterialTheme.extendedColors: ExtendedColors
    @Composable
    @ReadOnlyComposable
    get() = LocalExtendedColors.current

@Composable
fun SocialVideoDownloaderTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalExtendedColors provides SvdExtendedColors,
    ) {
        MaterialTheme(
            colorScheme = SvdColorScheme,
            typography = Typography,
            content = content,
        )
    }
}
