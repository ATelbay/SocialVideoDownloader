package com.socialvideodownloader.shared.ui.theme

import androidx.compose.runtime.Composable

/**
 * Android entry point for SvdTheme with Dynamic Color (Material You) support.
 * On Android 12+ (API 31+), applies dynamicLightColorScheme when dynamicColor = true.
 * Falls back to the static SVD warm editorial palette on older devices.
 */
@Composable
fun SvdThemeAndroid(
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    SvdTheme(dynamicColor = dynamicColor, content = content)
}
