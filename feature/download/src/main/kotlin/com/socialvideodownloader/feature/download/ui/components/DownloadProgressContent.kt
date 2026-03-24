package com.socialvideodownloader.feature.download.ui.components

import android.text.format.Formatter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.socialvideodownloader.core.domain.model.DownloadProgress
import com.socialvideodownloader.core.ui.theme.AppShapesInstance
import com.socialvideodownloader.core.ui.theme.SvdBorder
import com.socialvideodownloader.core.ui.theme.SvdBorderStrong
import com.socialvideodownloader.core.ui.theme.SvdForeground
import com.socialvideodownloader.core.ui.theme.SvdMutedForeground
import com.socialvideodownloader.core.ui.theme.SvdPrimary
import com.socialvideodownloader.core.ui.theme.SvdPrimaryStrong
import com.socialvideodownloader.core.ui.theme.SvdSurfaceAlt
import com.socialvideodownloader.core.ui.theme.SvdSurfaceStrong
import com.socialvideodownloader.core.ui.theme.StatsValue
import com.socialvideodownloader.core.ui.tokens.Spacing
import com.socialvideodownloader.feature.download.R

@Composable
fun DownloadProgressContent(
    progress: DownloadProgress,
    onCancelClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val animatedProgress by animateFloatAsState(
        targetValue = progress.progressPercent / 100f,
        label = "progress",
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // Progress card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(AppShapesInstance.cardLg)
                .background(SvdSurfaceAlt)
                .border(1.dp, SvdBorder, AppShapesInstance.cardLg)
                .padding(Spacing.ProgressCardPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Large percentage
            Text(
                text = "${progress.progressPercent.toInt()}%",
                style = MaterialTheme.typography.displayLarge,
                color = SvdPrimaryStrong,
            )

            // Time estimate
            Text(
                text = if (progress.isMuxing) {
                    stringResource(R.string.download_finalizing_status)
                } else {
                    stringResource(R.string.download_downloading_status)
                },
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = SvdMutedForeground,
            )

            // Progress bar
            if (progress.isMuxing) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Spacing.ProgressTrackHeight)
                        .clip(AppShapesInstance.pill),
                    color = SvdPrimary,
                    trackColor = SvdSurfaceStrong,
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Spacing.ProgressTrackHeight)
                        .clip(AppShapesInstance.pill)
                        .background(SvdSurfaceStrong),
                ) {
                    if (animatedProgress > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedProgress)
                                .fillMaxHeight()
                                .background(SvdPrimary, AppShapesInstance.pill),
                        )
                    }
                }
            }

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = if (progress.isMuxing) "—" else progress.totalBytes?.let { Formatter.formatFileSize(context, it) } ?: "—",
                    style = StatsValue,
                    color = SvdForeground,
                )
                Text(
                    text = if (progress.isMuxing || progress.speedBytesPerSec <= 0) "—" else formatSpeed(progress.speedBytesPerSec),
                    style = StatsValue,
                    color = SvdForeground,
                )
            }
        }

        // Cancel button — neutral borderStrong style
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(Spacing.SecondaryButtonHeight)
                .clip(AppShapesInstance.control)
                .border(1.dp, SvdBorderStrong, AppShapesInstance.control)
                .clickable(onClick = onCancelClicked),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.download_cancel_download),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = SvdForeground,
            )
        }
    }
}

private fun formatSpeed(bytesPerSec: Long): String {
    return when {
        bytesPerSec >= 1_048_576 -> "%.1f MB/s".format(bytesPerSec / 1_048_576.0)
        bytesPerSec >= 1024 -> "%.0f KB/s".format(bytesPerSec / 1024.0)
        else -> "$bytesPerSec B/s"
    }
}
