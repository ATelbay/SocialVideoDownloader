package com.socialvideodownloader.core.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// SVD Design Token Palette
val SvdBg = Color(0xFF0F0D15)
val SvdSurface = Color(0xFF1A1726)
val SvdSurfaceElevated = Color(0xFF241F33)
val SvdSurfaceBright = Color(0xFF2E2844)
val SvdPrimary = Color(0xFF8B5CF6)
val SvdPrimaryEnd = Color(0xFF7C3AED)
val SvdPrimarySoft = Color(0xFFA78BFA)
val SvdPrimaryContainer = Color(0xFF2D2150)
val SvdText = Color(0xFFFFFFFF)
val SvdTextSecondary = Color(0xFFA09BB0)
val SvdTextTertiary = Color(0xFF6B6580)
val SvdBorder = Color(0xFF2E2844)
val SvdSuccess = Color(0xFF6ECF83)
val SvdSuccessContainer = Color(0xFF1B3D25)
val SvdError = Color(0xFFFF6B6B)
val SvdErrorContainer = Color(0xFF3D1B1B)

val SvdColorScheme = darkColorScheme(
    background = SvdBg,
    surface = SvdSurface,
    surfaceContainer = SvdSurface,
    surfaceContainerHigh = SvdSurfaceElevated,
    surfaceContainerHighest = SvdSurfaceBright,
    primary = SvdPrimary,
    onPrimary = SvdText,
    primaryContainer = SvdPrimaryContainer,
    onPrimaryContainer = SvdPrimary,
    secondary = SvdPrimary,
    onSecondary = SvdText,
    secondaryContainer = SvdPrimaryContainer,
    onSecondaryContainer = SvdPrimary,
    tertiary = SvdPrimary,
    onTertiary = SvdText,
    tertiaryContainer = SvdPrimaryContainer,
    onTertiaryContainer = SvdPrimary,
    onBackground = SvdText,
    onSurface = SvdText,
    onSurfaceVariant = SvdTextSecondary,
    outline = SvdTextTertiary,
    outlineVariant = SvdBorder,
    error = SvdError,
    onError = SvdText,
    errorContainer = SvdErrorContainer,
    onErrorContainer = SvdError,
    inverseSurface = SvdText,
    inverseOnSurface = SvdBg,
    scrim = Color.Black,
)

// Extended colors: success palette not part of M3
@Immutable
data class ExtendedColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
)

val SvdExtendedColors = ExtendedColors(
    success = SvdSuccess,
    onSuccess = Color(0xFF003919),
    successContainer = SvdSuccessContainer,
    onSuccessContainer = SvdSuccess,
)

val LocalExtendedColors = staticCompositionLocalOf { SvdExtendedColors }
