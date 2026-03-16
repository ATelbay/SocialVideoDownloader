package com.socialvideodownloader.feature.download.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import com.socialvideodownloader.core.ui.theme.AppShapesInstance
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.socialvideodownloader.core.ui.components.GradientButton
import com.socialvideodownloader.core.ui.theme.SvdBorder
import com.socialvideodownloader.core.ui.theme.SvdPrimary
import com.socialvideodownloader.core.ui.theme.SvdPrimaryContainer
import com.socialvideodownloader.core.ui.theme.SvdSurface
import com.socialvideodownloader.core.ui.theme.SvdText
import com.socialvideodownloader.core.ui.theme.SvdTextSecondary
import com.socialvideodownloader.core.ui.theme.SvdTextTertiary
import com.socialvideodownloader.core.ui.tokens.PlatformColors
import com.socialvideodownloader.feature.download.R

private data class PlatformChipData(val name: String, val color: Color)

private val supportedPlatforms = listOf(
    PlatformChipData("YouTube", PlatformColors.YouTube),
    PlatformChipData("Instagram", PlatformColors.Instagram),
    PlatformChipData("TikTok", PlatformColors.TikTok),
    PlatformChipData("Twitter", PlatformColors.Twitter),
    PlatformChipData("Vimeo", PlatformColors.Vimeo),
    PlatformChipData("Facebook", PlatformColors.Facebook),
)

@Composable
fun IdleContent(
    url: String,
    onUrlChanged: (String) -> Unit,
    onExtractClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 28.dp, start = 24.dp, end = 24.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        // Hero section
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(SvdPrimaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    tint = SvdPrimary,
                    modifier = Modifier.size(36.dp),
                )
            }

            Text(
                text = stringResource(R.string.download_hero_title),
                style = MaterialTheme.typography.titleLarge.copy(color = SvdText),
                textAlign = TextAlign.Center,
            )

            Text(
                text = stringResource(R.string.download_hero_subtitle),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Normal,
                    color = SvdTextSecondary,
                ),
                textAlign = TextAlign.Center,
            )
        }

        UrlInputContent(
            url = url,
            onUrlChanged = onUrlChanged,
        )

        GradientButton(
            text = stringResource(R.string.download_extract_video),
            onClick = onExtractClicked,
            icon = Icons.Default.AutoAwesome,
        )

        // Supported platforms divider
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                thickness = 1.dp,
                color = SvdBorder,
            )
            Text(
                text = stringResource(R.string.download_supported_platforms_label),
                style = MaterialTheme.typography.labelSmall.copy(
                    color = SvdTextTertiary,
                    letterSpacing = 1.sp,
                ),
            )
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                thickness = 1.dp,
                color = SvdBorder,
            )
        }

        // Platform chips
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            supportedPlatforms.forEach { platform ->
                Row(
                    modifier = Modifier
                        .clip(AppShapesInstance.medium)
                        .background(SvdSurface)
                        .border(1.dp, SvdBorder, AppShapesInstance.medium)
                        .padding(vertical = 10.dp, horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(platform.color),
                    )
                    Text(
                        text = platform.name,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Medium,
                            color = SvdText,
                        ),
                    )
                }
            }
        }
    }
}
