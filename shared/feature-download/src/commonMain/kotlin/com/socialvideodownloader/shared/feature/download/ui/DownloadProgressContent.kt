package com.socialvideodownloader.shared.feature.download.ui

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.socialvideodownloader.core.domain.model.DownloadProgress
import com.socialvideodownloader.shared.feature.download.generated.resources.Res
import com.socialvideodownloader.shared.feature.download.generated.resources.action_cancel_download
import com.socialvideodownloader.shared.feature.download.generated.resources.progress_downloading
import com.socialvideodownloader.shared.feature.download.generated.resources.progress_finalizing
import com.socialvideodownloader.shared.ui.theme.LocalAppShapes
import com.socialvideodownloader.shared.ui.theme.Spacing
import com.socialvideodownloader.shared.ui.theme.SvdBorder
import com.socialvideodownloader.shared.ui.theme.SvdBorderStrong
import com.socialvideodownloader.shared.ui.theme.SvdForeground
import com.socialvideodownloader.shared.ui.theme.SvdMutedForeground
import com.socialvideodownloader.shared.ui.theme.SvdPrimary
import com.socialvideodownloader.shared.ui.theme.SvdPrimaryStrong
import com.socialvideodownloader.shared.ui.theme.SvdSurfaceAlt
import com.socialvideodownloader.shared.ui.theme.SvdSurfaceStrong
import org.jetbrains.compose.resources.stringResource

@Composable
fun DownloadProgressContent(
    progress: DownloadProgress,
    onCancelClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shapes = LocalAppShapes.current
    val animatedProgress by animateFloatAsState(
        targetValue = progress.progressPercent / 100f,
        label = "progress",
    )
    val labelCancelDownload = stringResource(Res.string.action_cancel_download)
    val labelDownloading = stringResource(Res.string.progress_downloading)
    val labelFinalizing = stringResource(Res.string.progress_finalizing)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // Progress card
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(shapes.cardLg)
                    .background(SvdSurfaceAlt)
                    .border(1.dp, SvdBorder, shapes.cardLg)
                    .padding(Spacing.ProgressCardPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "${progress.progressPercent.toInt()}%",
                style = MaterialTheme.typography.displayLarge,
                color = SvdPrimaryStrong,
            )

            Text(
                text = if (progress.isMuxing) labelFinalizing else labelDownloading,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = SvdMutedForeground,
            )

            if (progress.isMuxing) {
                LinearProgressIndicator(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(Spacing.ProgressTrackHeight)
                            .clip(shapes.pill),
                    color = SvdPrimary,
                    trackColor = SvdSurfaceStrong,
                )
            } else {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(Spacing.ProgressTrackHeight)
                            .clip(shapes.pill)
                            .background(SvdSurfaceStrong),
                ) {
                    if (animatedProgress > 0f) {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth(animatedProgress)
                                    .fillMaxHeight()
                                    .background(SvdPrimary, shapes.pill),
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text =
                        if (progress.isMuxing) {
                            "—"
                        } else {
                            progress.totalBytes?.let { formatFileSize(it) } ?: "—"
                        },
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                    color = SvdForeground,
                )
                Text(
                    text =
                        if (progress.isMuxing || progress.speedBytesPerSec <= 0) {
                            "—"
                        } else {
                            formatSpeed(progress.speedBytesPerSec)
                        },
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                    color = SvdForeground,
                )
            }
        }

        // Cancel button
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = Spacing.SecondaryButtonHeight)
                    .clip(shapes.control)
                    .border(1.dp, SvdBorderStrong, shapes.control)
                    .semantics(mergeDescendants = true) {
                        role = Role.Button
                        contentDescription = labelCancelDownload
                    }
                    .clickable(onClick = onCancelClicked),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = labelCancelDownload,
                style =
                    MaterialTheme.typography.labelLarge.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                color = SvdForeground,
            )
        }
    }
}

private fun formatSpeed(bytesPerSec: Long): String =
    when {
        bytesPerSec >= 1_048_576L -> "${roundOneDecimal(bytesPerSec / 1_048_576.0)} MB/s"
        bytesPerSec >= 1_024L -> "${bytesPerSec / 1_024L} KB/s"
        else -> "$bytesPerSec B/s"
    }

private fun roundOneDecimal(value: Double): String {
    val scaled = (value * 10).toLong()
    return "${scaled / 10}.${scaled % 10}"
}
