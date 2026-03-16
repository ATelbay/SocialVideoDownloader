package com.socialvideodownloader.feature.history.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import com.socialvideodownloader.core.ui.theme.AppShapesInstance
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.socialvideodownloader.core.ui.theme.SvdPrimary
import com.socialvideodownloader.core.ui.theme.SvdPrimaryEnd
import com.socialvideodownloader.core.ui.theme.SvdSurfaceElevated
import com.socialvideodownloader.core.ui.theme.SvdText
import com.socialvideodownloader.core.ui.theme.SvdTextSecondary
import com.socialvideodownloader.core.ui.theme.SvdTextTertiary
import com.socialvideodownloader.feature.history.R

@Composable
fun HistoryEmptyState(
    isSearchResult: Boolean,
    onStartDownloading: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(SvdSurfaceElevated),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isSearchResult) Icons.Outlined.Search else Icons.Outlined.Schedule,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = SvdTextTertiary,
            )
        }

        Text(
            text = stringResource(R.string.history_empty_title),
            style = MaterialTheme.typography.headlineSmall,
            color = SvdText,
            textAlign = TextAlign.Center,
        )

        Text(
            text = stringResource(
                if (isSearchResult) R.string.history_no_results_description else R.string.history_empty_description_new,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = SvdTextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        if (!isSearchResult) {
            Box(
                modifier = Modifier
                    .clip(AppShapesInstance.cardSm)
                    .background(Brush.verticalGradient(listOf(SvdPrimary, SvdPrimaryEnd)))
                    .clickable(onClick = onStartDownloading)
                    .padding(vertical = 12.dp, horizontal = 24.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        tint = SvdText,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = stringResource(R.string.history_start_downloading),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = SvdText,
                    )
                }
            }
        }
    }
}
