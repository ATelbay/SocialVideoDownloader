package com.socialvideodownloader.feature.download.ui.components

import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.socialvideodownloader.core.ui.theme.SectionLabel
import com.socialvideodownloader.core.domain.model.VideoFormatOption
import com.socialvideodownloader.core.ui.components.FormatChip
import com.socialvideodownloader.core.ui.theme.AppShapesInstance
import com.socialvideodownloader.core.ui.theme.SvdBorder
import com.socialvideodownloader.core.ui.theme.SvdForeground
import com.socialvideodownloader.core.ui.theme.SvdSubtleForeground
import com.socialvideodownloader.core.ui.theme.SvdSurfaceAlt
import com.socialvideodownloader.core.ui.theme.StatsValue
import com.socialvideodownloader.core.ui.tokens.Spacing
import com.socialvideodownloader.feature.download.R

@Composable
fun FormatChipsContent(
    formats: List<VideoFormatOption>,
    selectedFormatId: String,
    onFormatSelected: (String) -> Unit,
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
        verticalArrangement = Arrangement.spacedBy(Spacing.SectionGap),
    ) {
        // VIDEO QUALITY label
        if (videoFormats.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.download_video_quality_label),
                    style = SectionLabel,
                    color = SvdSubtleForeground,
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.ChipRowGap),
                    contentPadding = PaddingValues(horizontal = 0.dp),
                ) {
                    items(videoFormats, key = { it.formatId }) { format ->
                        FormatChip(
                            label = formatChipLabel(format),
                            selected = format.formatId == selectedFormatId,
                            onClick = { onFormatSelected(format.formatId) },
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
                    style = SectionLabel,
                    color = SvdSubtleForeground,
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.ChipRowGap),
                    contentPadding = PaddingValues(horizontal = 0.dp),
                ) {
                    items(audioFormats, key = { it.formatId }) { format ->
                        FormatChip(
                            label = formatChipLabel(format),
                            selected = format.formatId == selectedFormatId,
                            onClick = { onFormatSelected(format.formatId) },
                        )
                    }
                }
            }
        }

        // Format summary bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(AppShapesInstance.summary)
                .background(SvdSurfaceAlt)
                .border(1.dp, SvdBorder, AppShapesInstance.summary)
                .padding(Spacing.SummaryBarPadding),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.download_selected_format),
                style = SectionLabel,
                color = SvdSubtleForeground,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = selectedFormat?.label ?: "",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = SvdForeground,
                )
                Text(
                    text = selectedFormat?.fileSizeBytes?.let { bytes ->
                        Formatter.formatFileSize(context, bytes)
                    } ?: stringResource(R.string.download_format_info_unknown_size),
                    style = StatsValue,
                    color = SvdForeground,
                )
            }
        }

    }
}

private fun formatChipLabel(format: VideoFormatOption): String {
    return if (format.resolution != null) {
        "${format.resolution}p · ${format.ext.uppercase()}"
    } else {
        format.label
    }
}
