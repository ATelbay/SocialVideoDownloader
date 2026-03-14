package com.socialvideodownloader.feature.download.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.socialvideodownloader.core.domain.model.VideoMetadata
import com.socialvideodownloader.feature.download.R

@Composable
fun DownloadCompleteContent(
    metadata: VideoMetadata,
    onOpenClicked: () -> Unit,
    onShareClicked: () -> Unit,
    onNewDownloadClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        VideoInfoContent(metadata = metadata)
        Spacer(modifier = Modifier.height(16.dp))
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.download_complete),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = onOpenClicked,
                modifier = Modifier.weight(1f),
            ) {
                Text(text = stringResource(R.string.download_open))
            }
            OutlinedButton(
                onClick = onShareClicked,
                modifier = Modifier.weight(1f),
            ) {
                Text(text = stringResource(R.string.download_share))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(
            onClick = onNewDownloadClicked,
        ) {
            Text(text = stringResource(R.string.download_new))
        }
    }
}
