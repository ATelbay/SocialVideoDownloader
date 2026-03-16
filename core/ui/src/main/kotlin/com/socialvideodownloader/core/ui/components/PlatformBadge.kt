package com.socialvideodownloader.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.socialvideodownloader.core.ui.theme.AppShapesInstance
import com.socialvideodownloader.core.ui.theme.SocialVideoDownloaderTheme
import com.socialvideodownloader.core.ui.tokens.PlatformColors

@Composable
fun PlatformBadge(
    platformName: String,
    platformColor: Color,
    modifier: Modifier = Modifier,
    abbreviation: Boolean = false,
) {
    val text = if (abbreviation) PlatformColors.abbreviation(platformName) else platformName
    val textColor = PlatformColors.textColor(platformName)
    val shape = if (abbreviation) AppShapesInstance.badge else AppShapesInstance.badgeLg
    val textStyle = if (abbreviation) {
        MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold)
    } else {
        MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
    val padding = if (abbreviation) {
        PaddingValues(horizontal = 6.dp, vertical = 2.dp)
    } else {
        PaddingValues(horizontal = 8.dp, vertical = 2.dp)
    }

    Text(
        text = text,
        style = textStyle.copy(color = textColor),
        modifier = modifier
            .clip(shape)
            .background(platformColor)
            .padding(padding),
    )
}

@Preview(showBackground = true)
@Composable
private fun PlatformBadgeFullPreview() {
    SocialVideoDownloaderTheme {
        PlatformBadge(
            platformName = "YouTube",
            platformColor = PlatformColors.YouTube,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PlatformBadgeAbbreviationPreview() {
    SocialVideoDownloaderTheme {
        PlatformBadge(
            platformName = "TikTok",
            platformColor = PlatformColors.TikTok,
            abbreviation = true,
        )
    }
}
