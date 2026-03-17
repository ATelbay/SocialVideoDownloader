package com.socialvideodownloader.core.ui.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// SVD Warm Editorial Palette
val SvdBg = Color(0xFFF6F3EC)
val SvdSurface = Color(0xFFFFFFFC) // near-white #FFFDFC approximated
val SvdSurfaceAlt = Color(0xFFFAF6EE)
val SvdSurfaceStrong = Color(0xFFF0EBE0)
val SvdCard = Color(0xFFFFFDFC)
val SvdPrimary = Color(0xFFF26B3A)
val SvdPrimaryStrong = Color(0xFFD95222)
val SvdPrimarySoft = Color(0xFFFFE0D2)
val SvdWarning = Color(0xFFF2B84B)
val SvdAccent = Color(0xFF1E8C7A)
val SvdAccentSoft = Color(0xFFD9F1EC)
val SvdForeground = Color(0xFF1F2328)
val SvdMutedForeground = Color(0xFF5E6672)
val SvdSubtleForeground = Color(0xFF7D8794)
val SvdBorder = Color(0xFFD7D0C4)
val SvdBorderStrong = Color(0xFFB6AA97)
val SvdSuccess = Color(0xFF2D9D66)
val SvdSuccessSoft = Color(0xFFDDF4E8)
val SvdError = Color(0xFFD9534F)
val SvdErrorSoft = Color(0xFFFDE5E3)
val SvdShadow = Color(0x1A000000)

val SvdColorScheme = lightColorScheme(
    background = SvdBg,
    surface = SvdSurface,
    surfaceContainer = SvdSurfaceAlt,
    surfaceContainerHigh = SvdSurfaceStrong,
    surfaceContainerHighest = SvdSurfaceStrong,
    primary = SvdPrimary,
    onPrimary = Color.White,
    primaryContainer = SvdPrimarySoft,
    onPrimaryContainer = SvdPrimaryStrong,
    secondary = SvdAccent,
    onSecondary = Color.White,
    secondaryContainer = SvdAccentSoft,
    onSecondaryContainer = SvdAccent,
    tertiary = SvdWarning,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFF3D6),
    onTertiaryContainer = Color(0xFF8B6914),
    onBackground = SvdForeground,
    onSurface = SvdForeground,
    onSurfaceVariant = SvdMutedForeground,
    outline = SvdSubtleForeground,
    outlineVariant = SvdBorder,
    error = SvdError,
    onError = Color.White,
    errorContainer = SvdErrorSoft,
    onErrorContainer = SvdError,
    inverseSurface = SvdForeground,
    inverseOnSurface = SvdBg,
    scrim = Color.Black,
)

// Extended colors beyond M3
@Immutable
data class ExtendedColors(
    val surfaceAlt: Color,
    val surfaceStrong: Color,
    val card: Color,
    val primaryStrong: Color,
    val primarySoft: Color,
    val warning: Color,
    val accent: Color,
    val accentSoft: Color,
    val mutedForeground: Color,
    val subtleForeground: Color,
    val border: Color,
    val borderStrong: Color,
    val success: Color,
    val successSoft: Color,
    val errorSoft: Color,
    val shadow: Color,
)

val SvdExtendedColors = ExtendedColors(
    surfaceAlt = SvdSurfaceAlt,
    surfaceStrong = SvdSurfaceStrong,
    card = SvdCard,
    primaryStrong = SvdPrimaryStrong,
    primarySoft = SvdPrimarySoft,
    warning = SvdWarning,
    accent = SvdAccent,
    accentSoft = SvdAccentSoft,
    mutedForeground = SvdMutedForeground,
    subtleForeground = SvdSubtleForeground,
    border = SvdBorder,
    borderStrong = SvdBorderStrong,
    success = SvdSuccess,
    successSoft = SvdSuccessSoft,
    errorSoft = SvdErrorSoft,
    shadow = SvdShadow,
)

val LocalExtendedColors = staticCompositionLocalOf { SvdExtendedColors }
