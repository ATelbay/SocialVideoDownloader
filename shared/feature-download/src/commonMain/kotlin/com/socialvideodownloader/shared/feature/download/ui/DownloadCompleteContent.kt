package com.socialvideodownloader.shared.feature.download.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.socialvideodownloader.core.domain.model.VideoMetadata
import com.socialvideodownloader.shared.ui.components.TextActionLink
import com.socialvideodownloader.shared.ui.components.VideoInfoCard
import com.socialvideodownloader.shared.ui.theme.LocalAppShapes
import com.socialvideodownloader.shared.ui.theme.Spacing
import com.socialvideodownloader.shared.ui.theme.SvdBg
import com.socialvideodownloader.shared.ui.theme.SvdForeground
import com.socialvideodownloader.shared.ui.theme.SvdMutedForeground
import com.socialvideodownloader.shared.ui.theme.SvdPrimary
import com.socialvideodownloader.shared.ui.theme.SvdPrimaryStrong
import com.socialvideodownloader.shared.ui.theme.SvdSuccess
import com.socialvideodownloader.shared.ui.theme.SvdSuccessSoft
import com.socialvideodownloader.shared.ui.theme.SvdWarning
import com.socialvideodownloader.shared.ui.tokens.PlatformColors

@Composable
fun DownloadCompleteContent(
    metadata: VideoMetadata,
    onOpenClicked: () -> Unit,
    onShareClicked: () -> Unit,
    onNewDownloadClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shapes = LocalAppShapes.current
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        val platformName = PlatformColors.nameFromUrl(metadata.sourceUrl)
        VideoInfoCard(
            thumbnailUrl = metadata.thumbnailUrl,
            title = metadata.title,
            uploaderName = metadata.author,
            platformName = platformName,
            compact = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier =
                    Modifier
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
                text = "Download Complete!",
                style = MaterialTheme.typography.headlineMedium,
                color = SvdForeground,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Saved to your Downloads folder",
                style = MaterialTheme.typography.bodyMedium,
                color = SvdMutedForeground,
                textAlign = TextAlign.Center,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Open button (outlined)
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = Spacing.SecondaryButtonHeight)
                        .clip(shapes.control)
                        .border(1.dp, SvdPrimaryStrong, shapes.control)
                        .semantics(mergeDescendants = true) {
                            role = Role.Button
                            contentDescription = "Open"
                        }
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
                        text = "Open",
                        style = MaterialTheme.typography.labelLarge,
                        color = SvdPrimaryStrong,
                    )
                }
            }

            // Share button (gradient fill)
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = Spacing.SecondaryButtonHeight)
                        .clip(shapes.control)
                        .background(Brush.verticalGradient(listOf(SvdPrimary, SvdWarning)))
                        .semantics(mergeDescendants = true) {
                            role = Role.Button
                            contentDescription = "Share"
                        }
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
                        tint = SvdBg,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = "Share",
                        style = MaterialTheme.typography.labelLarge,
                        color = SvdBg,
                    )
                }
            }
        }

        TextActionLink(
            text = "New Download",
            onClick = onNewDownloadClicked,
        )
    }
}
