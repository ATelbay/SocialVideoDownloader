package com.socialvideodownloader.feature.download.ui.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.socialvideodownloader.core.ui.theme.AppShapesInstance
import com.socialvideodownloader.core.ui.tokens.PlatformColors
import com.socialvideodownloader.core.ui.tokens.Spacing
import com.socialvideodownloader.feature.download.R

private val supportedPlatforms = listOf("YouTube", "Instagram", "TikTok", "Twitter", "Vimeo", "Facebook")

private fun detectPlatformFromUrl(url: String): String? {
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

@Composable
fun IdleContent(
    url: String,
    onUrlChanged: (String) -> Unit,
    onExtractClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val detectedPlatform = remember(url) { if (url.isNotBlank()) detectPlatformFromUrl(url) else null }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(Spacing.HeroTopPadding))

        // Hero section
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Download,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(36.dp),
            )
        }

        Spacer(modifier = Modifier.height(Spacing.SectionGap))

        Text(
            text = stringResource(R.string.download_hero_title),
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            ),
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = stringResource(R.string.download_hero_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(Spacing.SectionGap))

        UrlInputContent(
            url = url,
            onUrlChanged = onUrlChanged,
            onExtractClicked = onExtractClicked,
            isLoading = false,
        )

        if (url.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            val platformName = detectedPlatform
            if (platformName != null) {
                Text(
                    text = platformName,
                    style = MaterialTheme.typography.bodySmall,
                    color = PlatformColors.forPlatform(platformName),
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.SectionGap))

        Button(
            onClick = onExtractClicked,
            enabled = url.isNotBlank(),
            shape = AppShapesInstance.medium,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
        ) {
            Text(text = stringResource(R.string.download_extract_button))
        }

        Spacer(modifier = Modifier.height(Spacing.SectionGap))

        // Supported platforms section
        Text(
            text = stringResource(R.string.download_supported_platforms).uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(Spacing.ListItemGap))

        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.ListItemGap),
            modifier = Modifier.fillMaxWidth(),
        ) {
            supportedPlatforms.forEach { platform ->
                Surface(
                    shape = AppShapesInstance.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Text(
                        text = platform,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}
