package com.socialvideodownloader.feature.download.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.socialvideodownloader.core.domain.model.VideoMetadata
import com.socialvideodownloader.core.ui.components.VideoInfoCard
import com.socialvideodownloader.core.ui.theme.AppShapesInstance
import com.socialvideodownloader.core.ui.theme.SvdPrimary
import com.socialvideodownloader.core.ui.theme.SvdPrimaryEnd
import com.socialvideodownloader.core.ui.theme.SvdSuccess
import com.socialvideodownloader.core.ui.theme.SvdSuccessContainer
import com.socialvideodownloader.core.ui.theme.SvdTextSecondary
import com.socialvideodownloader.core.ui.tokens.PlatformColors
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
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        // Compact VideoInfoCard
        val platformName = PlatformColors.nameFromUrl(metadata.sourceUrl)
        VideoInfoCard(
            thumbnailUrl = metadata.thumbnailUrl,
            title = metadata.title,
            uploaderName = metadata.author,
            platformName = platformName,
            platformColor = PlatformColors.forPlatform(platformName),
            compact = true,
            modifier = Modifier.fillMaxWidth(),
        )

        // Success section
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .background(SvdSuccessContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = SvdSuccess,
                    modifier = Modifier.size(40.dp),
                )
            }
            Text(
                text = stringResource(R.string.download_complete_message),
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.download_saved_message),
                style = MaterialTheme.typography.bodyMedium,
                color = SvdTextSecondary,
                textAlign = TextAlign.Center,
            )
        }

        // Action buttons
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Open button (outlined)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(AppShapesInstance.cardSm)
                    .border(1.5.dp, SvdPrimary, AppShapesInstance.cardSm)
                    .clickable(onClick = onOpenClicked),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.OpenInNew,
                        contentDescription = null,
                        tint = SvdPrimary,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = stringResource(R.string.download_open),
                        style = MaterialTheme.typography.labelLarge,
                        color = SvdPrimary,
                    )
                }
            }

            // Share button (gradient fill)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(AppShapesInstance.cardSm)
                    .background(Brush.verticalGradient(listOf(SvdPrimary, SvdPrimaryEnd)))
                    .clickable(onClick = onShareClicked),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = stringResource(R.string.download_share),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                    )
                }
            }
        }

        // New Download link
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(AppShapesInstance.cardSm)
                .clickable(onClick = onNewDownloadClicked)
                .padding(vertical = 10.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = null,
                tint = SvdPrimary,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = stringResource(R.string.download_new_download),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = SvdPrimary,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
    }
}

