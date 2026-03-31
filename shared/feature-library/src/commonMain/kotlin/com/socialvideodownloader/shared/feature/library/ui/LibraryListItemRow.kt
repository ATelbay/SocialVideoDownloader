package com.socialvideodownloader.shared.feature.library.ui

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.socialvideodownloader.shared.feature.library.LibraryListItem
import com.socialvideodownloader.shared.feature.library.platform.formatFileSize
import com.socialvideodownloader.shared.feature.library.platform.formatRelativeTime
import com.socialvideodownloader.shared.ui.components.PlatformBadge
import com.socialvideodownloader.shared.ui.theme.LocalAppShapes
import com.socialvideodownloader.shared.ui.theme.SvdBorder
import com.socialvideodownloader.shared.ui.theme.SvdForeground
import com.socialvideodownloader.shared.ui.theme.SvdSubtleForeground
import com.socialvideodownloader.shared.ui.theme.SvdSurface
import com.socialvideodownloader.shared.ui.theme.SvdSurfaceStrong
import com.socialvideodownloader.shared.ui.theme.Spacing

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryListItemRow(
    item: LibraryListItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shapes = LocalAppShapes.current
    val formattedDate = formatRelativeTime(item.completedAt)
    val formattedSize = item.fileSizeBytes?.let { formatFileSize(it) }

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(shapes.card)
                .background(SvdSurface)
                .border(1.dp, SvdBorder, shapes.card)
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .padding(Spacing.CardInnerPaddingCompact),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier =
                    Modifier
                        .width(Spacing.ThumbnailCompactWidth)
                        .height(Spacing.ThumbnailCompactHeight)
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
                    style = MaterialTheme.typography.bodyMedium,
                    color = SvdForeground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                if (item.platformName.isNotEmpty()) {
                    PlatformBadge(platformName = item.platformName)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    val formatLabel = item.formatLabel
                    if (formatLabel != null) {
                        Text(
                            text = formatLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = SvdSubtleForeground,
                        )
                        Text(
                            text = "·",
                            style = MaterialTheme.typography.bodySmall,
                            color = SvdSubtleForeground,
                        )
                    }
                    if (formattedSize != null) {
                        Text(
                            text = formattedSize,
                            style = MaterialTheme.typography.bodySmall,
                            color = SvdSubtleForeground,
                        )
                        Text(
                            text = "·",
                            style = MaterialTheme.typography.bodySmall,
                            color = SvdSubtleForeground,
                        )
                    }
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = SvdSubtleForeground,
                    )
                }
            }
        }
    }
}
