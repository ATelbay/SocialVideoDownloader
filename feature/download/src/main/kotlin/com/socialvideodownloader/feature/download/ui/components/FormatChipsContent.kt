package com.socialvideodownloader.feature.download.ui.components

import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.socialvideodownloader.core.domain.model.VideoFormatOption
import com.socialvideodownloader.core.ui.components.FormatChip
import com.socialvideodownloader.core.ui.components.GradientButton
import com.socialvideodownloader.core.ui.theme.AppShapesInstance
import com.socialvideodownloader.core.ui.theme.StatsValue
import com.socialvideodownloader.core.ui.theme.SvdPrimary
import com.socialvideodownloader.core.ui.theme.SvdSurfaceElevated
import com.socialvideodownloader.core.ui.theme.SvdTextTertiary
import com.socialvideodownloader.feature.download.R

@Composable
fun FormatChipsContent(
    formats: List<VideoFormatOption>,
    selectedFormatId: String,
    onFormatSelected: (String) -> Unit,
    onDownloadClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val videoFormats = remember(formats) { formats.filter { !it.isAudioOnly } }
    val audioFormats = remember(formats) { formats.filter { it.isAudioOnly } }
    val selectedFormat = remember(formats, selectedFormatId) {
        formats.find { it.formatId == selectedFormatId }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // VIDEO QUALITY label
        if (videoFormats.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.download_video_quality_label),
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                    color = SvdTextTertiary,
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 0.dp),
                ) {
                    items(videoFormats, key = { it.formatId }) { format ->
                        FormatChip(
                            label = formatChipLabel(format),
                            selected = format.formatId == selectedFormatId,
                            onClick = { onFormatSelected(format.formatId) },
                            isAudio = false,
                        )
                    }
                }
            }
        }

        // AUDIO QUALITY label
        if (audioFormats.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.download_audio_quality_label),
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                    color = SvdTextTertiary,
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 0.dp),
                ) {
                    items(audioFormats, key = { it.formatId }) { format ->
                        FormatChip(
                            label = formatChipLabel(format),
                            selected = format.formatId == selectedFormatId,
                            onClick = { onFormatSelected(format.formatId) },
                            isAudio = true,
                        )
                    }
                }
            }
        }

        // Format summary bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(AppShapesInstance.cardSm)
                .background(SvdSurfaceElevated)
                .padding(vertical = 14.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.download_selected_format),
                    style = MaterialTheme.typography.labelSmall,
                    color = SvdTextTertiary,
                )
                Text(
                    text = selectedFormat?.label ?: "",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                )
            }
            Text(
                text = selectedFormat?.fileSizeBytes?.let { bytes ->
                    Formatter.formatFileSize(context, bytes)
                } ?: stringResource(R.string.download_format_info_unknown_size),
                style = StatsValue,
                color = SvdPrimary,
            )
        }

        // Download button
        GradientButton(
            text = stringResource(R.string.download_button),
            onClick = onDownloadClicked,
            icon = Icons.Outlined.Download,
        )
    }
}

private fun formatChipLabel(format: VideoFormatOption): String {
    return if (format.resolution != null) {
        "${format.resolution}p · ${format.ext.uppercase()}"
    } else {
        format.label
    }
}
