package com.socialvideodownloader.shared.feature.download.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.socialvideodownloader.core.domain.model.ExistingDownload
import com.socialvideodownloader.shared.ui.theme.LocalAppShapes
import com.socialvideodownloader.shared.ui.theme.Spacing
import com.socialvideodownloader.shared.ui.theme.SvdAccent
import com.socialvideodownloader.shared.ui.theme.SvdBorder
import com.socialvideodownloader.shared.ui.theme.SvdForeground
import com.socialvideodownloader.shared.ui.theme.SvdSubtleForeground
import com.socialvideodownloader.shared.ui.theme.SvdSurface

@Composable
fun ExistingDownloadBanner(
    existingDownload: ExistingDownload,
    onOpenClicked: () -> Unit,
    onShareClicked: () -> Unit,
    onDismissClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shapes = LocalAppShapes.current
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = shapes.card,
        colors = CardDefaults.cardColors(containerColor = SvdSurface),
        border = BorderStroke(1.dp, SvdBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.CardInnerPaddingFull),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "ALREADY DOWNLOADED",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SvdAccent,
                    letterSpacing = 0.6.sp,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDismissClicked) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Dismiss",
                        tint = SvdSubtleForeground,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (existingDownload.thumbnailUrl != null) {
                    AsyncImage(
                        model = existingDownload.thumbnailUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier =
                            Modifier
                                .size(
                                    width = Spacing.ThumbnailCompactWidth,
                                    height = Spacing.ThumbnailCompactHeight,
                                )
                                .clip(shapes.thumbnail),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = existingDownload.videoTitle,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SvdForeground,
                        maxLines = 2,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = existingDownload.formatLabel,
                        fontSize = 12.sp,
                        color = SvdSubtleForeground,
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(
                    onClick = onOpenClicked,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FolderOpen,
                        contentDescription = null,
                        tint = SvdAccent,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Open",
                        color = SvdAccent,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                    )
                }
                TextButton(
                    onClick = onShareClicked,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = null,
                        tint = SvdSubtleForeground,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Share",
                        color = SvdSubtleForeground,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                    )
                }
            }
        }
    }
}
