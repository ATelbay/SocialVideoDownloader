package com.socialvideodownloader.feature.history.ui

import android.text.format.DateUtils
import android.text.format.Formatter
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.socialvideodownloader.core.domain.model.DownloadStatus
import com.socialvideodownloader.feature.history.R

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryListItemRow(
    item: HistoryListItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val isDimmed = item.status == DownloadStatus.FAILED || !item.isFileAccessible
    Row(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (isDimmed) 0.6f else 1f)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = item.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(width = 80.dp, height = 60.dp)
                .clip(RoundedCornerShape(4.dp)),
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.formatLabel ?: stringResource(R.string.history_format_unknown),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.width(8.dp))

                val (statusLabel, statusColor) = statusLabelAndColor(item.status)
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = DateUtils.getRelativeTimeSpanString(
                        item.createdAt,
                        System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS,
                    ).toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (item.fileSizeBytes != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = Formatter.formatFileSize(context, item.fileSizeBytes),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun statusLabelAndColor(status: DownloadStatus): Pair<String, Color> {
    return when (status) {
        DownloadStatus.COMPLETED -> Pair(
            stringResource(R.string.history_status_completed),
            MaterialTheme.colorScheme.primary,
        )
        DownloadStatus.FAILED -> Pair(
            stringResource(R.string.history_status_failed),
            MaterialTheme.colorScheme.error,
        )
        DownloadStatus.DOWNLOADING -> Pair(
            stringResource(R.string.history_status_downloading),
            MaterialTheme.colorScheme.tertiary,
        )
        DownloadStatus.PENDING, DownloadStatus.QUEUED -> Pair(
            stringResource(R.string.history_status_pending),
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
        DownloadStatus.CANCELLED -> Pair(
            stringResource(R.string.history_status_pending),
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
