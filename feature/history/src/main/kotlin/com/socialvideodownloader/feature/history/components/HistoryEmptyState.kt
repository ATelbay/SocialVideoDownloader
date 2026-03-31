package com.socialvideodownloader.feature.history.components

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.socialvideodownloader.core.ui.components.GradientButton
import com.socialvideodownloader.core.ui.theme.SvdForeground
import com.socialvideodownloader.core.ui.theme.SvdMutedForeground
import com.socialvideodownloader.core.ui.theme.SvdSubtleForeground
import com.socialvideodownloader.core.ui.theme.SvdSurfaceStrong
import com.socialvideodownloader.feature.history.R

@Composable
fun HistoryEmptyState(
    isSearchResult: Boolean,
    onStartDownloading: () -> Unit,
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
            text = stringResource(R.string.history_empty_title),
            style = MaterialTheme.typography.headlineSmall,
            color = SvdForeground,
            textAlign = TextAlign.Center,
        )

        Text(
            text =
                stringResource(
                    if (isSearchResult) R.string.history_no_results_description else R.string.history_empty_description_new,
                ),
            style = MaterialTheme.typography.bodyMedium,
            color = SvdMutedForeground,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        if (!isSearchResult) {
            GradientButton(
                text = stringResource(R.string.history_start_downloading),
                onClick = onStartDownloading,
                icon = Icons.Outlined.Download,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
