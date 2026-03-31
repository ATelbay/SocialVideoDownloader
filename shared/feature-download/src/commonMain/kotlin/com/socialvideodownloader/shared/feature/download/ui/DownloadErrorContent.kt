package com.socialvideodownloader.shared.feature.download.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.socialvideodownloader.shared.ui.components.GradientButton
import com.socialvideodownloader.shared.ui.components.TextActionLink
import com.socialvideodownloader.shared.ui.theme.Spacing
import com.socialvideodownloader.shared.ui.theme.SvdError
import com.socialvideodownloader.shared.ui.theme.SvdErrorSoft
import com.socialvideodownloader.shared.ui.theme.SvdForeground
import com.socialvideodownloader.shared.ui.theme.SvdMutedForeground

@Composable
fun DownloadErrorContent(
    message: String,
    onRetryClicked: () -> Unit,
    onNewDownloadClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(Spacing.HeroIconSize)
                        .background(SvdErrorSoft, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Warning,
                    contentDescription = null,
                    tint = SvdError,
                    modifier = Modifier.size(40.dp),
                )
            }
            Text(
                text = "Failed to extract",
                style = MaterialTheme.typography.headlineMedium,
                color = SvdForeground,
                textAlign = TextAlign.Center,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = SvdMutedForeground,
                textAlign = TextAlign.Center,
            )
        }

        GradientButton(
            text = "Retry",
            onClick = onRetryClicked,
            icon = Icons.Outlined.Refresh,
            modifier = Modifier.fillMaxWidth(),
        )

        TextActionLink(
            text = "New Download",
            onClick = onNewDownloadClicked,
        )
    }
}
