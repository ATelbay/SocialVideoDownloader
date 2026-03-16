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
import com.socialvideodownloader.core.ui.theme.AppShapesInstance
import com.socialvideodownloader.core.ui.theme.SocialVideoDownloaderTheme

@Composable
fun FormatChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    val backgroundColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }
    val labelColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal

    Surface(
        onClick = onClick,
        shape = AppShapesInstance.extraLarge,
        color = backgroundColor,
        border = BorderStroke(width = 2.dp, color = borderColor),
        modifier = modifier,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = fontWeight,
                color = labelColor,
            ),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
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
