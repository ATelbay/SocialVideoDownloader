package com.socialvideodownloader.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.OndemandVideo
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.SmartDisplay
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.socialvideodownloader.core.ui.theme.AppShapesInstance
import com.socialvideodownloader.core.ui.theme.SocialVideoDownloaderTheme
import com.socialvideodownloader.core.ui.theme.SvdBorder
import com.socialvideodownloader.core.ui.theme.SvdForeground
import com.socialvideodownloader.core.ui.theme.SvdPrimaryStrong
import com.socialvideodownloader.core.ui.theme.SvdSurfaceAlt
import com.socialvideodownloader.core.ui.tokens.Spacing

@Composable
fun PlatformBadge(
    platformName: String,
    modifier: Modifier = Modifier,
) {
    val icon = platformIcon(platformName)

    Row(
        modifier =
            modifier
                .clip(AppShapesInstance.pill)
                .background(SvdSurfaceAlt)
                .border(1.dp, SvdBorder, AppShapesInstance.pill)
                .padding(vertical = Spacing.ChipPaddingV, horizontal = Spacing.ChipPaddingH),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = SvdPrimaryStrong,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = platformName,
            style =
                MaterialTheme.typography.labelMedium.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
            color = SvdForeground,
        )
    }
}

private fun platformIcon(platformName: String): ImageVector =
    when {
        platformName.contains("youtube", ignoreCase = true) -> Icons.Outlined.SmartDisplay
        platformName.contains("instagram", ignoreCase = true) -> Icons.Outlined.CameraAlt
        platformName.contains("tiktok", ignoreCase = true) -> Icons.Outlined.MusicNote
        platformName.contains("twitter", ignoreCase = true) || platformName.contains("x.com", ignoreCase = true) -> Icons.Outlined.Public
        platformName.contains("vimeo", ignoreCase = true) -> Icons.Outlined.PlayCircle
        platformName.contains("facebook", ignoreCase = true) -> Icons.Outlined.OndemandVideo
        else -> Icons.Outlined.Public
    }

@Preview(showBackground = true)
@Composable
private fun PlatformBadgePreview() {
    SocialVideoDownloaderTheme {
        PlatformBadge(platformName = "YouTube")
    }
}

@Preview(showBackground = true)
@Composable
private fun PlatformBadgeTikTokPreview() {
    SocialVideoDownloaderTheme {
        PlatformBadge(platformName = "TikTok")
    }
}
