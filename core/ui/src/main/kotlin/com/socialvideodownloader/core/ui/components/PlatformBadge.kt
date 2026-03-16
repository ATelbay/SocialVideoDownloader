package com.socialvideodownloader.core.ui.components

import androidx.compose.foundation.background
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
) {
    Text(
        text = platformName,
        style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        ),
        modifier = modifier
            .clip(AppShapesInstance.extraLarge)
            .background(platformColor)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

@Preview(showBackground = true)
@Composable
private fun PlatformBadgePreview() {
    SocialVideoDownloaderTheme {
        PlatformBadge(
            platformName = "YouTube",
            platformColor = PlatformColors.YouTube,
        )
    }
}
