package com.socialvideodownloader.shared.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.socialvideodownloader.shared.ui.theme.SvdBg
import com.socialvideodownloader.shared.ui.theme.SvdTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * Design-system previews. Each component is rendered inside [SvdTheme] so the warm-editorial
 * palette, typography and shapes apply. These are tooling-only and have no runtime cost.
 */
@Preview
@Composable
private fun ButtonsPreview() {
    SvdTheme {
        Surface(color = SvdBg) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                GradientButton(text = "Download", onClick = {}, icon = Icons.Filled.Download)
                GradientButton(text = "Disabled", onClick = {}, enabled = false)
                SecondaryButton(text = "Secondary", onClick = {})
                TextActionLink(text = "Show all formats", onClick = {})
            }
        }
    }
}

@Preview
@Composable
private fun ChipsAndBadgesPreview() {
    SvdTheme {
        Surface(color = SvdBg) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FormatChip(label = "1080p", selected = true, onClick = {})
                FormatChip(label = "720p", selected = false, onClick = {})
                PlatformBadge(platformName = "YouTube")
                StatusBadge(status = "DOWNLOADING", label = "Downloading")
                StatusBadge(status = "COMPLETED", label = "Completed")
                StatusBadge(status = "FAILED", label = "Failed")
            }
        }
    }
}

@Preview
@Composable
private fun TopBarPreview() {
    SvdTheme {
        Surface(color = SvdBg) {
            SvdTopBar(
                title = "History",
                actionLabel = "Filter",
                onActionClick = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Preview
@Composable
private fun NavigationBarPreview() {
    SvdTheme {
        PillNavigationBar(
            items =
                listOf(
                    PillNavItem("Download", SvdNavIcons.Download),
                    PillNavItem("Library", SvdNavIcons.Library),
                    PillNavItem("History", SvdNavIcons.History),
                ),
            selectedIndex = 0,
            onSelect = {},
        )
    }
}

@Preview
@Composable
private fun VideoInfoCardPreview() {
    SvdTheme {
        Surface(color = SvdBg) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                VideoInfoCard(
                    thumbnailUrl = null,
                    title = "Sample video title that may wrap to two lines",
                    uploaderName = "Channel name",
                    durationSeconds = 215,
                    platformName = "YouTube",
                )
                VideoInfoCard(
                    thumbnailUrl = null,
                    title = "Compact card title",
                    uploaderName = "Channel name",
                    platformName = "TikTok",
                    compact = true,
                )
            }
        }
    }
}
