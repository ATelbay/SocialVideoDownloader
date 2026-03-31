package com.socialvideodownloader.feature.library.ui.components

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
import androidx.compose.material.icons.outlined.VideoLibrary
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
import com.socialvideodownloader.core.ui.theme.SvdSubtleForeground
import com.socialvideodownloader.core.ui.theme.SvdSurfaceStrong
import com.socialvideodownloader.feature.library.R

@Composable
fun LibraryEmptyState(
    onNavigateToDownload: () -> Unit,
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
                imageVector = Icons.Outlined.VideoLibrary,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = SvdSubtleForeground,
            )
        }

        Text(
            text = stringResource(R.string.library_empty_title),
            style = MaterialTheme.typography.headlineSmall,
            color = SvdForeground,
            textAlign = TextAlign.Center,
        )

        Text(
            text = stringResource(R.string.library_empty_description),
            style = MaterialTheme.typography.bodyMedium,
            color = SvdSubtleForeground,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        GradientButton(
            text = stringResource(R.string.library_start_downloading),
            onClick = onNavigateToDownload,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
