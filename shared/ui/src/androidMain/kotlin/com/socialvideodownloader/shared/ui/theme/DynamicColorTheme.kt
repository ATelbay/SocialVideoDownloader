package com.socialvideodownloader.shared.ui.theme

import androidx.compose.runtime.Composable

/**
 * Android entry point for SvdTheme with optional Dynamic Color (Material You) support.
 * Defaults to the static SVD warm-editorial brand palette. Opt in with dynamicColor = true
 * to let Android 12+ (API 31+) derive colors from the wallpaper instead — note this overrides
 * the brand-orange primary, so only enable it where wallpaper-tinted theming is desired.
 */
@Composable
fun SvdThemeAndroid(
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    SvdTheme(dynamicColor = dynamicColor, content = content)
}
