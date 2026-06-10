package com.socialvideodownloader.shared.feature.history.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.socialvideodownloader.core.domain.model.DownloadStatus
import com.socialvideodownloader.shared.feature.history.HistoryListItem
import com.socialvideodownloader.shared.feature.history.generated.resources.Res
import com.socialvideodownloader.shared.feature.history.generated.resources.status_cancelled
import com.socialvideodownloader.shared.feature.history.generated.resources.status_completed
import com.socialvideodownloader.shared.feature.history.generated.resources.status_downloading
import com.socialvideodownloader.shared.feature.history.generated.resources.status_failed
import com.socialvideodownloader.shared.feature.history.generated.resources.status_pending
import com.socialvideodownloader.shared.feature.history.generated.resources.status_queued
import com.socialvideodownloader.shared.ui.components.StatusBadge
import com.socialvideodownloader.shared.ui.theme.LocalAppShapes
import com.socialvideodownloader.shared.ui.theme.Spacing
import com.socialvideodownloader.shared.ui.theme.SvdBorder
import com.socialvideodownloader.shared.ui.theme.SvdForeground
import com.socialvideodownloader.shared.ui.theme.SvdMutedForeground
import com.socialvideodownloader.shared.ui.theme.SvdSubtleForeground
import com.socialvideodownloader.shared.ui.theme.SvdSurface
import com.socialvideodownloader.shared.ui.theme.SvdSurfaceStrong
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryListItemRow(
    item: HistoryListItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    formattedDate: String,
    formattedSize: String?,
    modifier: Modifier = Modifier,
) {
    val shapes = LocalAppShapes.current

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(shapes.card)
                .background(SvdSurface)
                .border(1.dp, SvdBorder, shapes.card)
                .semantics(mergeDescendants = true) {
                    role = Role.Button
                    contentDescription =
                        historyItemDescription(
                            title = item.title,
                            host = extractHost(item.sourceUrl),
                            formattedDate = formattedDate,
                            formattedSize = formattedSize,
                            status = item.status.name,
                        )
                }
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .padding(Spacing.CardInnerPaddingCompact),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier =
                    Modifier
                        .size(Spacing.ThumbnailCompactHeight)
                        .clip(shapes.thumbnail)
                        .background(SvdSurfaceStrong),
            ) {
                AsyncImage(
                    model = item.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier =
                        Modifier
                            .matchParentSize()
                            .clip(shapes.thumbnail),
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = SvdForeground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                val host = extractHost(item.sourceUrl)
                if (host != null) {
                    Text(
                        text = host,
                        style = MaterialTheme.typography.labelSmall,
                        color = SvdSubtleForeground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.labelMedium,
                        color = SvdMutedForeground,
                    )
                    if (formattedSize != null) {
                        Text(
                            text = "·",
                            style = MaterialTheme.typography.labelMedium,
                            color = SvdSubtleForeground,
                        )
                        Text(
                            text = formattedSize,
                            style = MaterialTheme.typography.labelMedium,
                            color = SvdMutedForeground,
                        )
                    }
                }

                StatusBadge(status = item.status.name, label = statusLabel(item.status))
            }
        }
    }
}

@Composable
private fun statusLabel(status: DownloadStatus): String =
    when (status) {
        DownloadStatus.COMPLETED -> stringResource(Res.string.status_completed)
        DownloadStatus.FAILED -> stringResource(Res.string.status_failed)
        DownloadStatus.DOWNLOADING -> stringResource(Res.string.status_downloading)
        DownloadStatus.PENDING -> stringResource(Res.string.status_pending)
        DownloadStatus.QUEUED -> stringResource(Res.string.status_queued)
        DownloadStatus.CANCELLED -> stringResource(Res.string.status_cancelled)
    }

private fun extractHost(url: String): String? {
    return try {
        val withoutScheme = url.substringAfter("://").substringBefore("/")
        withoutScheme.removePrefix("www.").takeIf { it.isNotBlank() }
    } catch (_: Exception) {
        null
    }
}

private fun historyItemDescription(
    title: String,
    host: String?,
    formattedDate: String,
    formattedSize: String?,
    status: String,
): String =
    listOfNotNull(
        title.takeIf { it.isNotBlank() },
        host?.takeIf { it.isNotBlank() },
        formattedDate.takeIf { it.isNotBlank() },
        formattedSize?.takeIf { it.isNotBlank() },
        status.lowercase().replace('_', ' '),
    ).joinToString(separator = ", ")
