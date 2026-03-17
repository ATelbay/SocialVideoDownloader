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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.OpenInNew
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.socialvideodownloader.core.domain.model.VideoMetadata
import com.socialvideodownloader.core.ui.components.TextActionLink
import com.socialvideodownloader.core.ui.components.VideoInfoCard
import com.socialvideodownloader.core.ui.theme.AppShapesInstance
import com.socialvideodownloader.core.ui.theme.SvdForeground
import com.socialvideodownloader.core.ui.theme.SvdMutedForeground
import com.socialvideodownloader.core.ui.theme.SvdPrimary
import com.socialvideodownloader.core.ui.theme.SvdPrimaryStrong
import com.socialvideodownloader.core.ui.theme.SvdSuccess
import com.socialvideodownloader.core.ui.theme.SvdSuccessSoft
import com.socialvideodownloader.core.ui.theme.SvdWarning
import com.socialvideodownloader.core.ui.tokens.PlatformColors
import com.socialvideodownloader.core.ui.tokens.Spacing
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
                    .size(Spacing.HeroIconSize)
                    .background(SvdSuccessSoft, CircleShape),
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
                color = SvdForeground,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.download_saved_message),
                style = MaterialTheme.typography.bodyMedium,
                color = SvdMutedForeground,
                textAlign = TextAlign.Center,
            )
        }

        // Action buttons
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Open button (outlined)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Spacing.SecondaryButtonHeight)
                    .clip(AppShapesInstance.control)
                    .border(1.dp, SvdPrimaryStrong, AppShapesInstance.control)
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
                        tint = SvdPrimaryStrong,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = stringResource(R.string.download_open),
                        style = MaterialTheme.typography.labelLarge,
                        color = SvdPrimaryStrong,
                    )
                }
            }

            // Share button (gradient fill)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Spacing.SecondaryButtonHeight)
                    .clip(AppShapesInstance.control)
                    .background(Brush.verticalGradient(listOf(SvdPrimary, SvdWarning)))
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

        TextActionLink(
            text = stringResource(R.string.download_new_download),
            onClick = onNewDownloadClicked,
        )
    }
}
