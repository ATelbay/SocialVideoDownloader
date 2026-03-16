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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.socialvideodownloader.core.domain.model.DownloadStatus
import com.socialvideodownloader.core.ui.R
import com.socialvideodownloader.core.ui.theme.AppShapesInstance
import com.socialvideodownloader.core.ui.theme.SvdError
import com.socialvideodownloader.core.ui.theme.SvdErrorContainer
import com.socialvideodownloader.core.ui.theme.SvdPrimary
import com.socialvideodownloader.core.ui.theme.SvdPrimaryContainer
import com.socialvideodownloader.core.ui.theme.SvdSuccess
import com.socialvideodownloader.core.ui.theme.SvdSuccessContainer
import com.socialvideodownloader.core.ui.theme.SvdTextSecondary
import com.socialvideodownloader.core.ui.theme.SvdSurface

@Composable
fun StatusBadge(
    status: DownloadStatus,
    modifier: Modifier = Modifier,
) {
    val (containerColor, contentColor, label) = when (status) {
        DownloadStatus.COMPLETED -> Triple(
            SvdSuccessContainer,
            SvdSuccess,
            stringResource(R.string.status_completed),
        )
        DownloadStatus.FAILED -> Triple(
            SvdErrorContainer,
            SvdError,
            stringResource(R.string.status_failed),
        )
        DownloadStatus.DOWNLOADING -> Triple(
            SvdPrimaryContainer,
            SvdPrimary,
            stringResource(R.string.status_downloading),
        )
        DownloadStatus.PENDING -> Triple(
            SvdSurface,
            SvdTextSecondary,
            stringResource(R.string.status_pending),
        )
        DownloadStatus.QUEUED -> Triple(
            SvdSurface,
            SvdTextSecondary,
            stringResource(R.string.status_queued),
        )
        DownloadStatus.CANCELLED -> Triple(
            SvdSurface,
            SvdTextSecondary,
            stringResource(R.string.status_cancelled),
        )
    }

    Row(
        modifier = modifier
            .clip(AppShapesInstance.badge)
            .background(containerColor)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (status == DownloadStatus.DOWNLOADING) {
            AnimatedSpinnerDot(color = contentColor)
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
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
