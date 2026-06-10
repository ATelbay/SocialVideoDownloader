package com.socialvideodownloader.shared.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

@Immutable
data class AppShapes(
    val card: RoundedCornerShape = RoundedCornerShape(22.dp),
    val cardLg: RoundedCornerShape = RoundedCornerShape(24.dp),
    // Top-only rounding matching [cardLg], for edge-to-edge media at the top of a card.
    val cardTop: RoundedCornerShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    val control: RoundedCornerShape = RoundedCornerShape(18.dp),
    val summary: RoundedCornerShape = RoundedCornerShape(20.dp),
    val pill: RoundedCornerShape = RoundedCornerShape(999.dp),
    val navTab: RoundedCornerShape = RoundedCornerShape(26.dp),
    val thumbnail: RoundedCornerShape = RoundedCornerShape(16.dp),
    val small: RoundedCornerShape = RoundedCornerShape(8.dp),
)

val LocalAppShapes = staticCompositionLocalOf { AppShapes() }
