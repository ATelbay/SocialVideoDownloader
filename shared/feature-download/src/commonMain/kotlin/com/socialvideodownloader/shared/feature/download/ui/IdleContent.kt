package com.socialvideodownloader.shared.feature.download.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.socialvideodownloader.core.domain.model.ExistingDownload
import com.socialvideodownloader.shared.ui.components.GradientButton
import com.socialvideodownloader.shared.ui.components.PlatformBadge
import com.socialvideodownloader.shared.ui.theme.Spacing
import com.socialvideodownloader.shared.ui.theme.SvdForeground
import com.socialvideodownloader.shared.ui.theme.SvdMutedForeground
import com.socialvideodownloader.shared.ui.theme.SvdPrimarySoft
import com.socialvideodownloader.shared.ui.theme.SvdPrimaryStrong

private val supportedPlatforms = listOf("YouTube", "Instagram", "TikTok", "Twitter", "Vimeo", "Facebook")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IdleContent(
    url: String,
    onUrlChanged: (String) -> Unit,
    onExtractClicked: () -> Unit,
    modifier: Modifier = Modifier,
    existingDownload: ExistingDownload? = null,
    onOpenExistingClicked: () -> Unit = {},
    onShareExistingClicked: () -> Unit = {},
    onDismissExistingBanner: () -> Unit = {},
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(
                    top = Spacing.ContentTopPadding,
                    start = Spacing.ScreenPadding,
                    end = Spacing.ScreenPadding,
                    bottom = Spacing.ScreenPadding,
                ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.SectionGapIdle),
    ) {
        // Hero section
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(Spacing.HeroIconSize)
                        .clip(CircleShape)
                        .background(SvdPrimarySoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Download,
                    contentDescription = null,
                    tint = SvdPrimaryStrong,
                    modifier = Modifier.size(34.dp),
                )
            }

            Text(
                text = "Download Videos\nFrom Anywhere",
                style =
                    MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = SvdForeground,
                    ),
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 280.dp),
            )

            Text(
                text = "Paste any social media URL to get started",
                style =
                    MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Normal,
                        color = SvdMutedForeground,
                    ),
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 300.dp),
            )
        }

        UrlInputContent(
            url = url,
            onUrlChanged = onUrlChanged,
        )

        if (existingDownload != null) {
            ExistingDownloadBanner(
                existingDownload = existingDownload,
                onOpenClicked = onOpenExistingClicked,
                onShareClicked = onShareExistingClicked,
                onDismissClicked = onDismissExistingBanner,
            )
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.ChipRowGap),
            verticalArrangement = Arrangement.spacedBy(Spacing.ChipRowGap),
        ) {
            supportedPlatforms.forEach { platform ->
                PlatformBadge(platformName = platform)
            }
        }

        GradientButton(
            text = "EXTRACT VIDEO",
            onClick = onExtractClicked,
            icon = Icons.Outlined.Download,
            modifier = Modifier.fillMaxWidth(),
        )

        Text(
            text = "Supports 1700+ sites",
            style =
                MaterialTheme.typography.labelMedium.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                ),
            color = SvdMutedForeground,
            textAlign = TextAlign.Center,
        )
    }
}
