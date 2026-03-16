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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.socialvideodownloader.core.domain.model.DownloadStatus
import com.socialvideodownloader.core.ui.components.PlatformBadge
import com.socialvideodownloader.core.ui.components.StatusBadge
import com.socialvideodownloader.core.ui.theme.AppShapesInstance
import com.socialvideodownloader.core.ui.theme.SvdBorder
import com.socialvideodownloader.core.ui.theme.SvdSurface
import com.socialvideodownloader.core.ui.theme.SvdSurfaceElevated
import com.socialvideodownloader.core.ui.theme.SvdText
import com.socialvideodownloader.core.ui.theme.SvdTextSecondary
import com.socialvideodownloader.core.ui.theme.SvdTextTertiary
import com.socialvideodownloader.core.ui.tokens.PlatformColors
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
    val isFailed = item.status == DownloadStatus.FAILED

    Box(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (isFailed) 0.6f else 1f)
            .clip(AppShapesInstance.large)
            .background(SvdSurface)
            .border(1.dp, SvdBorder, AppShapesInstance.large)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Thumbnail with platform badge overlay at bottom-start
            val platformName = PlatformColors.nameFromUrl(item.sourceUrl)
            Box(modifier = Modifier.size(width = Spacing.ThumbnailCompactWidth, height = Spacing.ThumbnailCompactHeight)) {
                AsyncImage(
                    model = item.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .matchParentSize()
                        .clip(AppShapesInstance.small),
                )
                if (platformName != null) {
                    PlatformBadge(
                        platformName = platformName,
                        platformColor = PlatformColors.forPlatform(platformName),
                        abbreviation = true,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 2.dp, bottom = 2.dp),
                    )
                }
            }

            // Info column
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = Spacing.InnerGap),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                // Title
                Text(
                    text = item.title,
                    style = androidx.compose.material3.MaterialTheme.typography.titleSmall.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = SvdText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                // Format badge + StatusBadge row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    val formatLabel = item.formatLabel
                    if (formatLabel != null) {
                        Text(
                            text = formatLabel,
                            style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = SvdTextSecondary,
                            modifier = Modifier
                                .clip(AppShapesInstance.badge)
                                .background(SvdSurfaceElevated)
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                    StatusBadge(status = item.status)
                }

                // Timestamp + separator + file size row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = DateUtils.getRelativeTimeSpanString(
                            item.createdAt,
                            System.currentTimeMillis(),
                            DateUtils.MINUTE_IN_MILLIS,
                        ).toString(),
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = SvdTextTertiary,
                    )
                    Text(
                        text = "·",
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = SvdTextTertiary,
                    )
                    val sizeText = if (item.fileSizeBytes != null && !isFailed) {
                        Formatter.formatFileSize(context, item.fileSizeBytes)
                    } else {
                        "—"
                    }
                    Text(
                        text = sizeText,
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = SvdTextTertiary,
                    )
                }
            }
        }
    }
}
