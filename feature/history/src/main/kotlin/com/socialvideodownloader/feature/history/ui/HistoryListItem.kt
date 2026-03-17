package com.socialvideodownloader.feature.history.ui

import android.text.format.DateUtils
import android.text.format.Formatter
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.socialvideodownloader.core.ui.components.StatusBadge
import com.socialvideodownloader.core.ui.theme.AppShapesInstance
import com.socialvideodownloader.core.ui.theme.SvdBorder
import com.socialvideodownloader.core.ui.theme.SvdForeground
import com.socialvideodownloader.core.ui.theme.SvdMutedForeground
import com.socialvideodownloader.core.ui.theme.SvdSubtleForeground
import com.socialvideodownloader.core.ui.theme.SvdSurface
import com.socialvideodownloader.core.ui.theme.SvdSurfaceStrong
import com.socialvideodownloader.core.ui.tokens.Spacing

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryListItemRow(
    item: HistoryListItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppShapesInstance.card)
            .background(SvdSurface)
            .border(1.dp, SvdBorder, AppShapesInstance.card)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(Spacing.CardInnerPaddingCompact),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Thumbnail placeholder 72x72
            Box(
                modifier = Modifier
                    .size(Spacing.ThumbnailHistorySize)
                    .clip(AppShapesInstance.thumbnail)
                    .background(SvdSurfaceStrong),
            ) {
                AsyncImage(
                    model = item.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .matchParentSize()
                        .clip(AppShapesInstance.thumbnail),
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Info column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                // Title
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = SvdForeground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                // Meta + status row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = DateUtils.getRelativeTimeSpanString(
                            item.createdAt,
                            System.currentTimeMillis(),
                            DateUtils.MINUTE_IN_MILLIS,
                        ).toString(),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp),
                        color = SvdMutedForeground,
                    )
                    val sizeText = item.fileSizeBytes?.let { Formatter.formatFileSize(context, it) }
                    if (sizeText != null) {
                        Text(
                            text = "·",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp),
                            color = SvdSubtleForeground,
                        )
                        Text(
                            text = sizeText,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp),
                            color = SvdMutedForeground,
                        )
                    }
                }

                StatusBadge(status = item.status)
            }
        }
    }
}
