package com.socialvideodownloader.shared.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.socialvideodownloader.shared.ui.theme.LocalAppShapes
import com.socialvideodownloader.shared.ui.theme.Spacing
import com.socialvideodownloader.shared.ui.theme.SvdAccent
import com.socialvideodownloader.shared.ui.theme.SvdAccentSoft
import com.socialvideodownloader.shared.ui.theme.SvdError
import com.socialvideodownloader.shared.ui.theme.SvdErrorSoft
import com.socialvideodownloader.shared.ui.theme.SvdPrimarySoft
import com.socialvideodownloader.shared.ui.theme.SvdPrimaryStrong
import com.socialvideodownloader.shared.ui.theme.SvdSuccess
import com.socialvideodownloader.shared.ui.theme.SvdSuccessSoft

/**
 * Displays a colored status chip.
 *
 * @param status A string representation of the download status. Recognised values:
 *   "COMPLETED", "FAILED", "DOWNLOADING", "PENDING", "QUEUED", "CANCELLED".
 *   Unknown values are rendered with a neutral accent color.
 * @param isAnimated When true, a pulsing dot is shown (used for active downloads).
 */
@Composable
fun StatusBadge(
    status: String,
    isAnimated: Boolean = status.equals("DOWNLOADING", ignoreCase = true),
    modifier: Modifier = Modifier,
) {
    val shapes = LocalAppShapes.current
    val (containerColor, contentColor, label) =
        when (status.uppercase()) {
            "COMPLETED" -> Triple(SvdSuccessSoft, SvdSuccess, "Completed")
            "FAILED" -> Triple(SvdErrorSoft, SvdError, "Failed")
            "DOWNLOADING" -> Triple(SvdPrimarySoft, SvdPrimaryStrong, "Downloading")
            "PENDING" -> Triple(SvdAccentSoft, SvdAccent, "Pending")
            "QUEUED" -> Triple(SvdAccentSoft, SvdAccent, "Queued")
            "CANCELLED" -> Triple(SvdAccentSoft, SvdAccent, "Cancelled")
            else -> Triple(SvdAccentSoft, SvdAccent, status)
        }

    Row(
        modifier =
            modifier
                .height(Spacing.StatusChipHeight)
                .clip(shapes.pill)
                .background(containerColor)
                .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (isAnimated) {
            AnimatedSpinnerDot(color = contentColor)
        }
        Text(
            text = label,
            style =
                MaterialTheme.typography.labelSmall.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
            color = contentColor,
        )
    }
}

@Composable
private fun AnimatedSpinnerDot(color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "spinner_dot")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.2f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 600, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "dot_alpha",
    )

    Box(
        modifier =
            Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = alpha)),
    )
}
