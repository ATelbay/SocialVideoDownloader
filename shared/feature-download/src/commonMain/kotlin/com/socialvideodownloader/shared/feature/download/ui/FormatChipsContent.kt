package com.socialvideodownloader.shared.feature.download.ui

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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.socialvideodownloader.core.domain.model.VideoFormatOption
import com.socialvideodownloader.shared.ui.components.FormatChip
import com.socialvideodownloader.shared.ui.theme.LocalAppShapes
import com.socialvideodownloader.shared.ui.theme.Spacing
import com.socialvideodownloader.shared.ui.theme.SvdBorder
import com.socialvideodownloader.shared.ui.theme.SvdForeground
import com.socialvideodownloader.shared.ui.theme.SvdSubtleForeground
import com.socialvideodownloader.shared.ui.theme.SvdSurfaceAlt

@Composable
fun FormatChipsContent(
    formats: List<VideoFormatOption>,
    selectedFormatId: String,
    onFormatSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shapes = LocalAppShapes.current
    val videoFormats = remember(formats) { formats.filter { !it.isAudioOnly } }
    val audioFormats = remember(formats) { formats.filter { it.isAudioOnly } }
    val selectedFormat =
        remember(formats, selectedFormatId) {
            formats.find { it.formatId == selectedFormatId }
        }

    val sectionLabelStyle =
        TextStyle(
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            letterSpacing = 1.sp,
        )
    val statsStyle =
        TextStyle(
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
        )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.SectionGap),
    ) {
        if (videoFormats.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "VIDEO QUALITY",
                    style = sectionLabelStyle,
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

        if (audioFormats.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "AUDIO QUALITY",
                    style = sectionLabelStyle,
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
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(shapes.summary)
                    .background(SvdSurfaceAlt)
                    .border(1.dp, SvdBorder, shapes.summary)
                    .padding(Spacing.SummaryBarPadding),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "Selected Format",
                style = sectionLabelStyle,
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
                    text = selectedFormat?.fileSizeBytes?.let { formatFileSize(it) } ?: "Unknown size",
                    style = statsStyle,
                    color = SvdForeground,
                )
            }
        }
    }
}

private fun formatChipLabel(format: VideoFormatOption): String =
    if (format.resolution != null) {
        "${format.resolution}p \u00b7 ${format.ext.uppercase()}"
    } else {
        format.label
    }

internal fun formatFileSize(bytes: Long): String =
    when {
        bytes >= 1_073_741_824L -> "${roundOneDecimal(bytes / 1_073_741_824.0)} GB"
        bytes >= 1_048_576L -> "${roundOneDecimal(bytes / 1_048_576.0)} MB"
        bytes >= 1_024L -> "${(bytes / 1_024L)} KB"
        else -> "$bytes B"
    }

private fun roundOneDecimal(value: Double): String {
    val scaled = (value * 10).toLong()
    return "${scaled / 10}.${scaled % 10}"
}
