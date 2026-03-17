package com.socialvideodownloader.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.socialvideodownloader.core.ui.theme.AppShapesInstance
import com.socialvideodownloader.core.ui.theme.SocialVideoDownloaderTheme
import com.socialvideodownloader.core.ui.theme.SvdAccent
import com.socialvideodownloader.core.ui.theme.SvdBorder
import com.socialvideodownloader.core.ui.theme.SvdForeground
import com.socialvideodownloader.core.ui.theme.SvdMutedForeground
import com.socialvideodownloader.core.ui.theme.SvdPrimarySoft
import com.socialvideodownloader.core.ui.theme.SvdPrimaryStrong
import com.socialvideodownloader.core.ui.theme.SvdSurface
import com.socialvideodownloader.core.ui.tokens.PlatformColors
import com.socialvideodownloader.core.ui.tokens.Spacing

@Composable
fun VideoInfoCard(
    thumbnailUrl: String?,
    title: String,
    uploaderName: String? = null,
    durationSeconds: Int? = null,
    platformName: String? = null,
    platformColor: Color? = null,
    compact: Boolean = false,
    modifier: Modifier = Modifier,
) {
    if (compact) {
        CompactVideoInfoCard(
            thumbnailUrl = thumbnailUrl,
            title = title,
            uploaderName = uploaderName,
            platformName = platformName,
            modifier = modifier,
        )
    } else {
        FullVideoInfoCard(
            thumbnailUrl = thumbnailUrl,
            title = title,
            uploaderName = uploaderName,
            durationSeconds = durationSeconds,
            platformName = platformName,
            modifier = modifier,
        )
    }
}

@Composable
private fun FullVideoInfoCard(
    thumbnailUrl: String?,
    title: String,
    uploaderName: String?,
    durationSeconds: Int?,
    platformName: String?,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = AppShapesInstance.cardLg,
        colors = CardDefaults.cardColors(containerColor = SvdSurface),
        border = BorderStroke(1.dp, SvdBorder),
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(Spacing.CardInnerPaddingFull)) {
            // Thumbnail area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Spacing.ThumbnailFullHeight)
                    .clip(AppShapesInstance.control),
            ) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .matchParentSize()
                        .clip(AppShapesInstance.control)
                        .background(SvdPrimarySoft),
                )

                // Play button overlay
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .align(Alignment.Center)
                        .background(Color(0x33000000), AppShapesInstance.pill),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = SvdPrimaryStrong,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = SvdForeground,
            )

            if (uploaderName != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = uploaderName,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 13.sp,
                        lineHeight = (13 * 1.45).sp,
                    ),
                    color = SvdMutedForeground,
                )
            }

            // Chip row
            Spacer(modifier = Modifier.height(10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.ChipRowGap),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (platformName != null) {
                    PlatformBadge(platformName = platformName)
                }
            }
        }
    }
}

@Composable
private fun CompactVideoInfoCard(
    thumbnailUrl: String?,
    title: String,
    uploaderName: String?,
    platformName: String?,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = AppShapesInstance.card,
        colors = CardDefaults.cardColors(containerColor = SvdSurface),
        border = BorderStroke(1.dp, SvdBorder),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(Spacing.CardInnerPaddingCompact),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Thumbnail with teal bg
            Box(
                modifier = Modifier
                    .size(
                        width = Spacing.ThumbnailCompactWidth,
                        height = Spacing.ThumbnailCompactHeight,
                    )
                    .clip(AppShapesInstance.thumbnail)
                    .background(SvdAccent),
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .matchParentSize()
                        .clip(AppShapesInstance.thumbnail),
                )
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = SvdForeground,
                )

                if (uploaderName != null) {
                    Text(
                        text = uploaderName,
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp),
                        color = SvdMutedForeground,
                    )
                }
            }
        }
    }
}

private fun formatDuration(totalSeconds: Int): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}

@Preview(showBackground = true)
@Composable
private fun VideoInfoCardFullPreview() {
    SocialVideoDownloaderTheme {
        VideoInfoCard(
            thumbnailUrl = null,
            title = "Amazing video with a very long title",
            uploaderName = "Content Creator",
            durationSeconds = 305,
            platformName = "YouTube",
            compact = false,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun VideoInfoCardCompactPreview() {
    SocialVideoDownloaderTheme {
        VideoInfoCard(
            thumbnailUrl = null,
            title = "Short video title",
            uploaderName = "Creator",
            platformName = "Instagram",
            compact = true,
            modifier = Modifier.padding(16.dp),
        )
    }
}
