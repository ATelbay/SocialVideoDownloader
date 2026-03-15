package com.socialvideodownloader.feature.download.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.socialvideodownloader.core.domain.model.VideoMetadata
import com.socialvideodownloader.core.ui.components.VideoInfoCard
import com.socialvideodownloader.core.ui.theme.AppShapesInstance
import com.socialvideodownloader.core.ui.theme.extendedColors
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
    val extendedColors = MaterialTheme.extendedColors

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        VideoInfoCard(
            thumbnailUrl = metadata.thumbnailUrl,
            title = metadata.title,
            uploaderName = metadata.author,
            durationSeconds = metadata.durationSeconds,
            platformName = extractPlatformName(metadata.sourceUrl),
            platformColor = PlatformColors.forPlatform(metadata.sourceUrl),
            compact = false,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(Spacing.SectionGap))

        Box(
            modifier = Modifier
                .size(88.dp)
                .background(
                    color = extendedColors.successContainer,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = extendedColors.success,
                modifier = Modifier.size(52.dp),
            )
        }

        Spacer(modifier = Modifier.height(Spacing.SectionGap))

        Text(
            text = stringResource(R.string.download_complete_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = stringResource(R.string.download_complete_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(Spacing.SectionGap))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.ListItemGap),
        ) {
            OutlinedButton(
                onClick = onOpenClicked,
                shape = AppShapesInstance.medium,
                modifier = Modifier.weight(1f),
            ) {
                Text(text = stringResource(R.string.download_open))
            }
            Button(
                onClick = onShareClicked,
                shape = AppShapesInstance.medium,
                modifier = Modifier.weight(1f),
            ) {
                Text(text = stringResource(R.string.download_share))
            }
        }

        TextButton(onClick = onNewDownloadClicked) {
            Text(text = stringResource(R.string.download_new))
        }
    }
}

private fun extractPlatformName(url: String): String? {
    return when {
        url.contains("youtube.com", ignoreCase = true) || url.contains("youtu.be", ignoreCase = true) -> "YouTube"
        url.contains("instagram.com", ignoreCase = true) -> "Instagram"
        url.contains("tiktok.com", ignoreCase = true) -> "TikTok"
        url.contains("twitter.com", ignoreCase = true) || url.contains("x.com", ignoreCase = true) -> "Twitter"
        url.contains("vimeo.com", ignoreCase = true) -> "Vimeo"
        url.contains("facebook.com", ignoreCase = true) || url.contains("fb.watch", ignoreCase = true) -> "Facebook"
        else -> null
    }
}
