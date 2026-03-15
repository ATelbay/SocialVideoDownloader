package com.socialvideodownloader.core.ui.components

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
import androidx.compose.ui.unit.dp
import com.socialvideodownloader.core.domain.model.DownloadStatus
import com.socialvideodownloader.core.ui.theme.AppShapesInstance
import com.socialvideodownloader.core.ui.theme.extendedColors

private val DownloadingContainerColor = Color(0xFFDCEEFD)
private val DownloadingContentColor = Color(0xFF1565C0)
private val DownloadingContainerColorDark = Color(0xFF1A2E42)
private val DownloadingContentColorDark = Color(0xFF90CAF9)

@Composable
fun StatusBadge(
    status: DownloadStatus,
    modifier: Modifier = Modifier,
) {
    val isDark = MaterialTheme.colorScheme.surface.red < 0.5f

    val (containerColor, contentColor, label) = when (status) {
        DownloadStatus.COMPLETED -> Triple(
            MaterialTheme.extendedColors.successContainer,
            MaterialTheme.extendedColors.success,
            "Completed",
        )
        DownloadStatus.FAILED -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.error,
            "Failed",
        )
        DownloadStatus.DOWNLOADING -> Triple(
            if (isDark) DownloadingContainerColorDark else DownloadingContainerColor,
            if (isDark) DownloadingContentColorDark else DownloadingContentColor,
            "Downloading",
        )
        DownloadStatus.PENDING -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            "Pending",
        )
        DownloadStatus.QUEUED -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            "Queued",
        )
        DownloadStatus.CANCELLED -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            "Cancelled",
        )
    }

    Row(
        modifier = modifier
            .clip(AppShapesInstance.full)
            .background(containerColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (status == DownloadStatus.DOWNLOADING) {
            AnimatedSpinnerDot(color = contentColor)
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
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
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "dot_alpha",
    )

    Box(
        modifier = Modifier
            .size(6.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = alpha)),
    )
}
