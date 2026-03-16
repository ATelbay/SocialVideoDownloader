package com.socialvideodownloader.feature.download.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Warning
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
import com.socialvideodownloader.core.ui.components.GradientButton
import com.socialvideodownloader.core.ui.theme.AppShapesInstance
import com.socialvideodownloader.core.ui.theme.SvdError
import com.socialvideodownloader.core.ui.theme.SvdErrorContainer
import com.socialvideodownloader.core.ui.theme.SvdPrimary
import com.socialvideodownloader.core.ui.theme.SvdTextSecondary
import com.socialvideodownloader.feature.download.R

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
        // Error section
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .background(SvdErrorContainer, CircleShape),
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
                text = stringResource(R.string.download_error_title),
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = SvdTextSecondary,
                textAlign = TextAlign.Center,
            )
        }

        // Retry button
        GradientButton(
            text = stringResource(R.string.download_retry),
            onClick = onRetryClicked,
            icon = Icons.Outlined.Refresh,
        )

        // New Download link
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(AppShapesInstance.cardSm)
                .clickable(onClick = onNewDownloadClicked)
                .padding(vertical = 10.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = null,
                tint = SvdPrimary,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = stringResource(R.string.download_new_download),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = SvdPrimary,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
    }
}
