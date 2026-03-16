package com.socialvideodownloader.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.socialvideodownloader.core.ui.theme.AppShapesInstance
import com.socialvideodownloader.core.ui.theme.SocialVideoDownloaderTheme
import com.socialvideodownloader.core.ui.theme.SvdBorder
import com.socialvideodownloader.core.ui.theme.SvdPrimary
import com.socialvideodownloader.core.ui.theme.SvdPrimaryContainer
import com.socialvideodownloader.core.ui.theme.SvdSurface

@Composable
fun FormatChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isAudio: Boolean = false,
) {
    val borderColor = if (selected) SvdPrimary else SvdBorder
    val borderWidth = if (selected) 1.5.dp else 1.dp
    val backgroundColor = if (selected) SvdPrimaryContainer else SvdSurface
    val labelColor = if (selected) SvdPrimary else Color.White
    val fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
    val horizontalPadding = if (isAudio) 14.dp else 16.dp

    Surface(
        onClick = onClick,
        shape = AppShapesInstance.medium,
        color = backgroundColor,
        border = BorderStroke(width = borderWidth, color = borderColor),
        modifier = modifier,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 13.sp,
                fontWeight = fontWeight,
                color = labelColor,
            ),
            modifier = Modifier.padding(horizontal = horizontalPadding, vertical = 10.dp),
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

@Preview(showBackground = true)
@Composable
private fun FormatChipAudioPreview() {
    SocialVideoDownloaderTheme {
        FormatChip(label = "MP3 320kbps", selected = true, onClick = {}, isAudio = true)
    }
}
