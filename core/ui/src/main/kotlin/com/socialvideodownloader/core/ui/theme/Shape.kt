package com.socialvideodownloader.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

data class AppShapes(
    val small: RoundedCornerShape = RoundedCornerShape(10.dp),
    val medium: RoundedCornerShape = RoundedCornerShape(12.dp),
    val large: RoundedCornerShape = RoundedCornerShape(16.dp),
    val extraLarge: RoundedCornerShape = RoundedCornerShape(20.dp),
    val full: RoundedCornerShape = RoundedCornerShape(24.dp),
    val pill: RoundedCornerShape = RoundedCornerShape(36.dp),
    val pillTab: RoundedCornerShape = RoundedCornerShape(26.dp),
    val cardSm: RoundedCornerShape = RoundedCornerShape(14.dp),
    val badge: RoundedCornerShape = RoundedCornerShape(6.dp),
    val badgeLg: RoundedCornerShape = RoundedCornerShape(8.dp),
    val progress: RoundedCornerShape = RoundedCornerShape(5.dp),
    val bottomSheet: RoundedCornerShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
)

val AppShapesInstance = AppShapes()
