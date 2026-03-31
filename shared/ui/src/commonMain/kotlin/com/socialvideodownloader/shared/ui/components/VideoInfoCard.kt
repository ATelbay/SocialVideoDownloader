package com.socialvideodownloader.shared.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.socialvideodownloader.shared.ui.theme.LocalAppShapes
import com.socialvideodownloader.shared.ui.theme.SvdAccent
import com.socialvideodownloader.shared.ui.theme.SvdBorder
import com.socialvideodownloader.shared.ui.theme.SvdForeground
import com.socialvideodownloader.shared.ui.theme.SvdMutedForeground
import com.socialvideodownloader.shared.ui.theme.SvdPrimarySoft
import com.socialvideodownloader.shared.ui.theme.SvdPrimaryStrong
import com.socialvideodownloader.shared.ui.theme.SvdSurface
import com.socialvideodownloader.shared.ui.theme.Spacing

@Composable
fun VideoInfoCard(
    thumbnailUrl: String?,
    title: String,
    uploaderName: String? = null,
    durationSeconds: Int? = null,
    platformName: String? = null,
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
    val shapes = LocalAppShapes.current
    Card(
        shape = shapes.cardLg,
        colors = CardDefaults.cardColors(containerColor = SvdSurface),
        border = BorderStroke(1.dp, SvdBorder),
        modifier = modifier,
    ) {
        Column {
            // Thumbnail area — edge-to-edge within card, top-only rounding
            val thumbnailShape =
                RoundedCornerShape(
                    topStart = 24.dp,
                    topEnd = 24.dp,
                    bottomStart = 0.dp,
                    bottomEnd = 0.dp,
                )
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(Spacing.ThumbnailFullHeight)
                        .clip(thumbnailShape),
            ) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier =
                        Modifier
                            .matchParentSize()
                            .clip(thumbnailShape)
                            .background(SvdPrimarySoft),
                )

                // Play button overlay
                Box(
                    modifier =
                        Modifier
                            .size(44.dp)
                            .align(Alignment.Center)
                            .background(Color(0x33000000), shapes.pill),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = SvdPrimaryStrong,
                        modifier = Modifier.size(26.dp),
                    )
                }

                // Platform badge overlay
                if (platformName != null) {
                    PlatformBadge(
                        platformName = platformName,
                        modifier =
                            Modifier
                                .align(Alignment.BottomStart)
                                .padding(10.dp),
                    )
                }
            }

            // Text content with padding
            Column(modifier = Modifier.padding(Spacing.CardInnerPaddingFull)) {
                Text(
                    text = title,
                    style =
                        MaterialTheme.typography.titleMedium.copy(
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
                        style =
                            MaterialTheme.typography.labelMedium.copy(
                                fontSize = 13.sp,
                                lineHeight = (13 * 1.45).sp,
                            ),
                        color = SvdMutedForeground,
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
    modifier: Modifier = Modifier,
) {
    val shapes = LocalAppShapes.current
    Card(
        shape = shapes.card,
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
                modifier =
                    Modifier
                        .size(
                            width = Spacing.ThumbnailCompactWidth,
                            height = Spacing.ThumbnailCompactHeight,
                        )
                        .clip(shapes.thumbnail)
                        .background(SvdAccent),
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier =
                        Modifier
                            .matchParentSize()
                            .clip(shapes.thumbnail),
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
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = SvdForeground,
                )

                if (uploaderName != null) {
                    Text(
                        text = uploaderName,
                        style = MaterialTheme.typography.labelMedium,
                        color = SvdMutedForeground,
                    )
                }
            }
        }
    }
}
