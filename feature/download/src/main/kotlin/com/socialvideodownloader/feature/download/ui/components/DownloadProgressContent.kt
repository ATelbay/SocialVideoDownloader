package com.socialvideodownloader.feature.download.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.socialvideodownloader.core.domain.model.DownloadProgress
import com.socialvideodownloader.core.ui.theme.AppShapesInstance
import com.socialvideodownloader.core.ui.theme.StatsValue
import com.socialvideodownloader.core.ui.theme.SvdError
import com.socialvideodownloader.core.ui.theme.SvdPrimary
import com.socialvideodownloader.core.ui.theme.SvdPrimarySoft
import com.socialvideodownloader.core.ui.theme.SvdSurfaceElevated
import com.socialvideodownloader.core.ui.theme.SvdTextSecondary
import com.socialvideodownloader.core.ui.theme.SvdTextTertiary
import com.socialvideodownloader.feature.download.R

@Composable
fun DownloadProgressContent(
    progress: DownloadProgress,
    onCancelClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.progressPercent / 100f,
        label = "progress",
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        // Progress section
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Large percentage
            Text(
                text = "${progress.progressPercent.toInt()}%",
                style = MaterialTheme.typography.displayLarge,
                color = SvdPrimary,
            )

            Text(
                text = stringResource(R.string.download_downloading_status),
                style = MaterialTheme.typography.bodyMedium,
                color = SvdTextSecondary,
            )

            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(AppShapesInstance.progress)
                    .background(SvdSurfaceElevated),
            ) {
                if (animatedProgress > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .fillMaxHeight()
                            .background(
                                Brush.verticalGradient(listOf(SvdPrimary, SvdPrimarySoft)),
                                AppShapesInstance.progress,
                            ),
                    )
                }
            }

            // Stats row: Speed, ETA, Size
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                StatColumn(
                    label = stringResource(R.string.download_speed),
                    value = if (progress.speedBytesPerSec > 0) formatSpeed(progress.speedBytesPerSec) else "—",
                    modifier = Modifier.weight(1f),
                )
                StatColumn(
                    label = stringResource(R.string.download_eta),
                    value = if (progress.etaSeconds > 0) formatEta(progress.etaSeconds) else "—",
                    modifier = Modifier.weight(1f),
                )
                StatColumn(
                    label = stringResource(R.string.download_size),
                    value = progress.totalBytes?.let { formatSize(it) } ?: "—",
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // Cancel button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(AppShapesInstance.cardSm)
                .border(1.5.dp, SvdError, AppShapesInstance.cardSm)
                .clickable(onClick = onCancelClicked),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.download_cancel_download),
                style = MaterialTheme.typography.labelLarge,
                color = SvdError,
            )
        }
    }
}

@Composable
private fun StatColumn(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = SvdTextTertiary,
        )
        Text(
            text = value,
            style = StatsValue,
            color = Color.White,
        )
    }
}

private fun formatSpeed(bytesPerSec: Long): String {
    return when {
        bytesPerSec >= 1_048_576 -> "%.1f MB/s".format(bytesPerSec / 1_048_576.0)
        bytesPerSec >= 1024 -> "%.0f KB/s".format(bytesPerSec / 1024.0)
        else -> "$bytesPerSec B/s"
    }
}

private fun formatEta(seconds: Long): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return "%d:%02d".format(minutes, secs)
}

private fun formatSize(bytes: Long): String {
    return when {
        bytes >= 1_073_741_824 -> "%.1f GB".format(bytes / 1_073_741_824.0)
        bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
        bytes >= 1024 -> "%.0f KB".format(bytes / 1024.0)
        else -> "$bytes B"
    }
}
