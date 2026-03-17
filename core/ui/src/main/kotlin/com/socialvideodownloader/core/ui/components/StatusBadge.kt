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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.socialvideodownloader.core.domain.model.DownloadStatus
import com.socialvideodownloader.core.ui.R
import com.socialvideodownloader.core.ui.theme.AppShapesInstance
import com.socialvideodownloader.core.ui.theme.SvdAccent
import com.socialvideodownloader.core.ui.theme.SvdAccentSoft
import com.socialvideodownloader.core.ui.theme.SvdError
import com.socialvideodownloader.core.ui.theme.SvdErrorSoft
import com.socialvideodownloader.core.ui.theme.SvdPrimarySoft
import com.socialvideodownloader.core.ui.theme.SvdPrimaryStrong
import com.socialvideodownloader.core.ui.theme.SvdSuccess
import com.socialvideodownloader.core.ui.theme.SvdSuccessSoft
import com.socialvideodownloader.core.ui.tokens.Spacing

@Composable
fun StatusBadge(
    status: DownloadStatus,
    modifier: Modifier = Modifier,
) {
    val (containerColor, contentColor, label) = when (status) {
        DownloadStatus.COMPLETED -> Triple(
            SvdSuccessSoft,
            SvdSuccess,
            stringResource(R.string.status_completed),
        )
        DownloadStatus.FAILED -> Triple(
            SvdErrorSoft,
            SvdError,
            stringResource(R.string.status_failed),
        )
        DownloadStatus.DOWNLOADING -> Triple(
            SvdPrimarySoft,
            SvdPrimaryStrong,
            stringResource(R.string.status_downloading),
        )
        DownloadStatus.PENDING, DownloadStatus.QUEUED -> Triple(
            SvdAccentSoft,
            SvdAccent,
            stringResource(
                if (status == DownloadStatus.PENDING) R.string.status_pending
                else R.string.status_queued,
            ),
        )
        DownloadStatus.CANCELLED -> Triple(
            SvdAccentSoft,
            SvdAccent,
            stringResource(R.string.status_cancelled),
        )
    }

    Row(
        modifier = modifier
            .height(Spacing.StatusChipHeight)
            .clip(AppShapesInstance.pill)
            .background(containerColor)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (status == DownloadStatus.DOWNLOADING) {
            AnimatedSpinnerDot(color = contentColor)
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
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
