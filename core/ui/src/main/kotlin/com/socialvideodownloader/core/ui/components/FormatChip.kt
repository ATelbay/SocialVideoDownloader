package com.socialvideodownloader.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.socialvideodownloader.core.ui.theme.AppShapesInstance
import com.socialvideodownloader.core.ui.theme.SocialVideoDownloaderTheme
import com.socialvideodownloader.core.ui.theme.SvdBorder
import com.socialvideodownloader.core.ui.theme.SvdForeground
import com.socialvideodownloader.core.ui.theme.SvdPrimarySoft
import com.socialvideodownloader.core.ui.theme.SvdPrimaryStrong
import com.socialvideodownloader.core.ui.theme.SvdSurface
import com.socialvideodownloader.core.ui.tokens.Spacing

@Composable
fun FormatChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = if (selected) SvdPrimarySoft else SvdSurface
    val border = if (selected) null else BorderStroke(1.dp, SvdBorder)
    val labelColor = if (selected) SvdPrimaryStrong else SvdForeground
    val fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold

    Surface(
        onClick = onClick,
        shape = AppShapesInstance.pill,
        color = backgroundColor,
        border = border,
        modifier = modifier,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 13.sp,
                fontWeight = fontWeight,
                color = labelColor,
            ),
            modifier = Modifier.padding(
                horizontal = Spacing.ChipPaddingH,
                vertical = Spacing.ChipPaddingV,
            ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FormatChipSelectedPreview() {
    SocialVideoDownloaderTheme {
        FormatChip(label = "MP4 1080p", selected = true, onClick = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun FormatChipUnselectedPreview() {
    SocialVideoDownloaderTheme {
        FormatChip(label = "MP4 720p", selected = false, onClick = {})
    }
}
