package com.socialvideodownloader.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.socialvideodownloader.core.ui.tokens.PlatformColors

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
            platformColor = platformColor,
            modifier = modifier,
        )
    } else {
        FullVideoInfoCard(
            thumbnailUrl = thumbnailUrl,
            title = title,
            uploaderName = uploaderName,
            durationSeconds = durationSeconds,
            platformName = platformName,
            platformColor = platformColor,
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
    platformColor: Color?,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = AppShapesInstance.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier,
    ) {
        Column {
            // Thumbnail area
            Box {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(
                            RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = 0.dp,
                                bottomEnd = 0.dp,
                            ),
                        ),
                )

                // Play button overlay
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.Center)
                        .clip(CircleShape)
                        .background(Color(0x80000000)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                }

                // Duration badge
                if (durationSeconds != null) {
                    Text(
                        text = formatDuration(durationSeconds),
                        style = MaterialTheme.typography.labelSmall.copy(color = Color.White),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xCC000000))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                // Platform badge
                if (platformName != null) {
                    PlatformBadge(
                        platformName = platformName,
                        platformColor = platformColor ?: PlatformColors.forPlatform(platformName),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                if (uploaderName != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = uploaderName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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
    platformColor: Color?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Thumbnail with platform badge overlay
        Box(modifier = Modifier.size(width = 72.dp, height = 54.dp)) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .clip(AppShapesInstance.small),
            )

            if (platformName != null) {
                Text(
                    text = platformName,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    ),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .clip(RoundedCornerShape(4.dp))
                        .background(platformColor ?: PlatformColors.forPlatform(platformName))
                        .padding(horizontal = 4.dp, vertical = 1.dp),
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            if (uploaderName != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = uploaderName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun VideoInfoCardFullPreview() {
    SocialVideoDownloaderTheme {
        VideoInfoCard(
            thumbnailUrl = null,
            title = "Amazing video with a very long title that should be clamped to two lines maximum",
            uploaderName = "Content Creator",
            durationSeconds = 305,
            platformName = "YouTube",
            platformColor = PlatformColors.YouTube,
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
            platformColor = PlatformColors.Instagram,
            compact = true,
            modifier = Modifier.padding(16.dp),
        )
    }
}
