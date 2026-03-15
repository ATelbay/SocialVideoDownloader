package com.socialvideodownloader.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

// Small (10dp): Thumbnails, small badges
// Medium (12dp): Buttons, dialogs, search fields
// Large (16dp): Cards, inputs, list items
// ExtraLarge (20dp): Chips, progress cards, format selectors
// Full (24dp): Bottom sheets, delete dialogs
data class AppShapes(
    val small: RoundedCornerShape = RoundedCornerShape(10.dp),
    val medium: RoundedCornerShape = RoundedCornerShape(12.dp),
    val large: RoundedCornerShape = RoundedCornerShape(16.dp),
    val extraLarge: RoundedCornerShape = RoundedCornerShape(20.dp),
    val full: RoundedCornerShape = RoundedCornerShape(24.dp),
)

val AppShapesInstance = AppShapes()
