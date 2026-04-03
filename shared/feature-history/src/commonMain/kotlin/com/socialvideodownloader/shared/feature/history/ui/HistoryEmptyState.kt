package com.socialvideodownloader.shared.feature.history.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.socialvideodownloader.shared.ui.components.GradientButton
import com.socialvideodownloader.shared.ui.theme.SvdForeground
import com.socialvideodownloader.shared.ui.theme.SvdMutedForeground
import com.socialvideodownloader.shared.ui.theme.SvdSubtleForeground
import com.socialvideodownloader.shared.ui.theme.SvdSurfaceStrong

@Composable
fun HistoryEmptyState(
    isSearchResult: Boolean,
    onStartDownloading: () -> Unit,
    emptyTitle: String,
    emptyDescription: String,
    noResultsDescription: String,
    startDownloadingLabel: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = 40.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier =
                Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(SvdSurfaceStrong),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isSearchResult) Icons.Outlined.Search else Icons.Outlined.Schedule,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = SvdSubtleForeground,
            )
        }

        Text(
            text = emptyTitle,
            style = MaterialTheme.typography.headlineSmall,
            color = SvdForeground,
            textAlign = TextAlign.Center,
        )

        Text(
            text = if (isSearchResult) noResultsDescription else emptyDescription,
            style = MaterialTheme.typography.bodyMedium,
            color = SvdMutedForeground,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        if (!isSearchResult) {
            GradientButton(
                text = startDownloadingLabel,
                onClick = onStartDownloading,
                icon = Icons.Outlined.Download,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
