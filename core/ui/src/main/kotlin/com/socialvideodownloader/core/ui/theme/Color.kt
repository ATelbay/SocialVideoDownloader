package com.socialvideodownloader.core.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Light palette
private val light_primary = Color(0xFF6750A4)
private val light_onPrimary = Color(0xFFFFFFFF)
private val light_primaryContainer = Color(0xFFEADDFF)
private val light_onPrimaryContainer = Color(0xFF21005D)
private val light_secondary = Color(0xFF625B71)
private val light_onSecondary = Color(0xFFFFFFFF)
private val light_secondaryContainer = Color(0xFFE8DEF8)
private val light_onSecondaryContainer = Color(0xFF1D192B)
private val light_surface = Color(0xFFFFFBFE)
private val light_onSurface = Color(0xFF1C1B1F)
private val light_surfaceVariant = Color(0xFFE7E0EC)
private val light_onSurfaceVariant = Color(0xFF49454F)
private val light_surfaceContainerLowest = Color(0xFFFFFFFF)
private val light_surfaceContainerLow = Color(0xFFF7F2FA)
private val light_surfaceContainer = Color(0xFFF3EDF7)
private val light_surfaceContainerHigh = Color(0xFFECE6F0)
private val light_surfaceContainerHighest = Color(0xFFE6E0E9)
private val light_outline = Color(0xFF79747E)
private val light_outlineVariant = Color(0xFFCAC4D0)
private val light_background = Color(0xFFFFFBFE)
private val light_onBackground = Color(0xFF1C1B1F)
private val light_error = Color(0xFFB3261E)
private val light_onError = Color(0xFFFFFFFF)
private val light_errorContainer = Color(0xFFF9DEDC)
private val light_onErrorContainer = Color(0xFF410E0B)
private val light_inverseSurface = Color(0xFF313033)
private val light_inverseOnSurface = Color(0xFFF4EFF4)
private val light_scrim = Color(0xFF000000)

// Dark palette
private val dark_primary = Color(0xFFD0BCFF)
private val dark_onPrimary = Color(0xFF381E72)
private val dark_primaryContainer = Color(0xFF4F378B)
private val dark_onPrimaryContainer = Color(0xFFEADDFF)
private val dark_secondary = Color(0xFFCCC2DC)
private val dark_onSecondary = Color(0xFF332D41)
private val dark_secondaryContainer = Color(0xFF4A4458)
private val dark_onSecondaryContainer = Color(0xFFE8DEF8)
private val dark_surface = Color(0xFF141218)
private val dark_onSurface = Color(0xFFE6E1E5)
private val dark_surfaceVariant = Color(0xFF49454F)
private val dark_onSurfaceVariant = Color(0xFFCAC4D0)
private val dark_surfaceContainerLowest = Color(0xFF0F0D13)
private val dark_surfaceContainerLow = Color(0xFF1D1B20)
private val dark_surfaceContainer = Color(0xFF211F26)
private val dark_surfaceContainerHigh = Color(0xFF2B2930)
private val dark_surfaceContainerHighest = Color(0xFF36343B)
private val dark_outline = Color(0xFF938F99)
private val dark_outlineVariant = Color(0xFF49454F)
private val dark_background = Color(0xFF141218)
private val dark_onBackground = Color(0xFFE6E1E5)
private val dark_error = Color(0xFFF2B8B5)
private val dark_onError = Color(0xFF601410)
private val dark_errorContainer = Color(0xFF8C1D18)
private val dark_onErrorContainer = Color(0xFFF9DEDC)
private val dark_inverseSurface = Color(0xFFE6E1E5)
private val dark_inverseOnSurface = Color(0xFF313033)
private val dark_scrim = Color(0xFF000000)

val LightColorScheme = lightColorScheme(
    primary = light_primary,
    onPrimary = light_onPrimary,
    primaryContainer = light_primaryContainer,
    onPrimaryContainer = light_onPrimaryContainer,
    secondary = light_secondary,
    onSecondary = light_onSecondary,
    secondaryContainer = light_secondaryContainer,
    onSecondaryContainer = light_onSecondaryContainer,
    surface = light_surface,
    onSurface = light_onSurface,
    surfaceVariant = light_surfaceVariant,
    onSurfaceVariant = light_onSurfaceVariant,
    surfaceContainerLowest = light_surfaceContainerLowest,
    surfaceContainerLow = light_surfaceContainerLow,
    surfaceContainer = light_surfaceContainer,
    surfaceContainerHigh = light_surfaceContainerHigh,
    surfaceContainerHighest = light_surfaceContainerHighest,
    outline = light_outline,
    outlineVariant = light_outlineVariant,
    background = light_background,
    onBackground = light_onBackground,
    error = light_error,
    onError = light_onError,
    errorContainer = light_errorContainer,
    onErrorContainer = light_onErrorContainer,
    inverseSurface = light_inverseSurface,
    inverseOnSurface = light_inverseOnSurface,
    scrim = light_scrim,
)

val DarkColorScheme = darkColorScheme(
    primary = dark_primary,
    onPrimary = dark_onPrimary,
    primaryContainer = dark_primaryContainer,
    onPrimaryContainer = dark_onPrimaryContainer,
    secondary = dark_secondary,
    onSecondary = dark_onSecondary,
    secondaryContainer = dark_secondaryContainer,
    onSecondaryContainer = dark_onSecondaryContainer,
    surface = dark_surface,
    onSurface = dark_onSurface,
    surfaceVariant = dark_surfaceVariant,
    onSurfaceVariant = dark_onSurfaceVariant,
    surfaceContainerLowest = dark_surfaceContainerLowest,
    surfaceContainerLow = dark_surfaceContainerLow,
    surfaceContainer = dark_surfaceContainer,
    surfaceContainerHigh = dark_surfaceContainerHigh,
    surfaceContainerHighest = dark_surfaceContainerHighest,
    outline = dark_outline,
    outlineVariant = dark_outlineVariant,
    background = dark_background,
    onBackground = dark_onBackground,
    error = dark_error,
    onError = dark_onError,
    errorContainer = dark_errorContainer,
    onErrorContainer = dark_onErrorContainer,
    inverseSurface = dark_inverseSurface,
    inverseOnSurface = dark_inverseOnSurface,
    scrim = dark_scrim,
)

// Extended colors: success palette not part of M3
@Immutable
data class ExtendedColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
)

val LightExtendedColors = ExtendedColors(
    success = Color(0xFF1B873D),
    onSuccess = Color(0xFFFFFFFF),
    successContainer = Color(0xFFC8F5D5),
    onSuccessContainer = Color(0xFF002112),
)

val DarkExtendedColors = ExtendedColors(
    success = Color(0xFF6ECF83),
    onSuccess = Color(0xFF003919),
    successContainer = Color(0xFF1B5E20),
    onSuccessContainer = Color(0xFFA7F3B7),
)

val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }
