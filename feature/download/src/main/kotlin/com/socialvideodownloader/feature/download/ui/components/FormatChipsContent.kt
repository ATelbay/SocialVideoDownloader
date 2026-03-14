package com.socialvideodownloader.feature.download.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.socialvideodownloader.core.domain.model.VideoFormatOption
import com.socialvideodownloader.feature.download.R

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FormatChipsContent(
    formats: List<VideoFormatOption>,
    selectedFormatId: String,
    onFormatSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val videoFormats = formats.filter { !it.isAudioOnly }
    val audioFormats = formats.filter { it.isAudioOnly }

    Column(modifier = modifier.fillMaxWidth()) {
        if (videoFormats.isNotEmpty()) {
            Text(
                text = stringResource(R.string.download_video_formats),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
            ) {
                videoFormats.forEach { format ->
                    FilterChip(
                        selected = format.formatId == selectedFormatId,
                        onClick = { onFormatSelected(format.formatId) },
                        label = { Text(formatChipLabel(format)) },
                        modifier = Modifier.padding(end = 8.dp, bottom = 4.dp),
                    )
                }
            }
        }

        if (audioFormats.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.download_audio_formats),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
            ) {
                audioFormats.forEach { format ->
                    FilterChip(
                        selected = format.formatId == selectedFormatId,
                        onClick = { onFormatSelected(format.formatId) },
                        label = { Text(formatChipLabel(format)) },
                        modifier = Modifier.padding(end = 8.dp, bottom = 4.dp),
                    )
                }
            }
        }
    }
}

private fun formatChipLabel(format: VideoFormatOption): String {
    val sizeText = format.fileSizeBytes?.let { bytes ->
        if (bytes >= 1_073_741_824) {
            " · %.1f GB".format(bytes / 1_073_741_824.0)
        } else {
            " · %.1f MB".format(bytes / 1_048_576.0)
        }
    } ?: ""
    return "${format.label}$sizeText"
}
