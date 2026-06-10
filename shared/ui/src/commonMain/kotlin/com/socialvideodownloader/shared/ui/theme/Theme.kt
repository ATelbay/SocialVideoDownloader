package com.socialvideodownloader.shared.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

val MaterialTheme.extendedColors: ExtendedColors
    @Composable
    @ReadOnlyComposable
    get() = LocalExtendedColors.current

val MaterialTheme.appShapes: AppShapes
    @Composable
    @ReadOnlyComposable
    get() = LocalAppShapes.current

/**
 * SVD theme. The warm-editorial brand is intentionally light-only — there is no dark color
 * scheme, and [resolveColorScheme] never consults `isSystemInDarkTheme()`. On Android,
 * [dynamicColor] may opt into a wallpaper-derived light scheme (Material You); elsewhere the
 * static brand palette is always used.
 */
@Composable
fun SvdTheme(
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = resolveColorScheme(dynamicColor)
    val typography = svdTypography()

    CompositionLocalProvider(
        LocalExtendedColors provides SvdExtendedColors,
        LocalAppShapes provides AppShapes(),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content,
        )
    }
}
