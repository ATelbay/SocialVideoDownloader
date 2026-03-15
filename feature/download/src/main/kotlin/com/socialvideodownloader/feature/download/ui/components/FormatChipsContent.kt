package com.socialvideodownloader.feature.download.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.socialvideodownloader.core.domain.model.VideoFormatOption
import com.socialvideodownloader.core.ui.components.FormatChip
import com.socialvideodownloader.core.ui.theme.AppShapesInstance
import com.socialvideodownloader.core.ui.tokens.Spacing
import com.socialvideodownloader.feature.download.R

@Composable
fun FormatChipsContent(
    formats: List<VideoFormatOption>,
    selectedFormatId: String,
    onFormatSelected: (String) -> Unit,
    onDownloadClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val videoFormats = remember(formats) { formats.filter { !it.isAudioOnly } }
    val audioFormats = remember(formats) { formats.filter { it.isAudioOnly } }
    val selectedFormat = remember(formats, selectedFormatId) {
        formats.find { it.formatId == selectedFormatId }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.download_select_quality),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(Spacing.ListItemGap))

        if (videoFormats.isNotEmpty()) {
            Text(
                text = stringResource(R.string.download_video_formats),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
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

        if (audioFormats.isNotEmpty()) {
            Spacer(modifier = Modifier.height(Spacing.SectionGap))
            Text(
                text = stringResource(R.string.download_audio_formats),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
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

        if (selectedFormat != null) {
            Spacer(modifier = Modifier.height(Spacing.SectionGap))
            Surface(
                shape = AppShapesInstance.medium,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.CardPaddingHorizontal, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = selectedFormat.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                        if (selectedFormat.resolution != null) {
                            Text(
                                text = "${selectedFormat.resolution}p · ${selectedFormat.ext.uppercase()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                            )
                        }
                    }
                    Text(
                        text = selectedFormat.fileSizeBytes?.let { bytes ->
                            if (bytes >= 1_073_741_824) {
                                stringResource(R.string.download_file_size_gb, bytes / 1_073_741_824.0)
                            } else {
                                stringResource(R.string.download_file_size_mb, bytes / 1_048_576.0)
                            }
                        } ?: stringResource(R.string.download_format_info_unknown_size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.SectionGap))

        Button(
            onClick = onDownloadClicked,
            modifier = Modifier.fillMaxWidth(),
            shape = AppShapesInstance.medium,
        ) {
            Text(text = stringResource(R.string.download_button))
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
