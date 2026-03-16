package com.socialvideodownloader.feature.download.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.socialvideodownloader.core.ui.theme.AppShapesInstance
import com.socialvideodownloader.core.ui.theme.SvdBorder
import com.socialvideodownloader.core.ui.theme.SvdError
import com.socialvideodownloader.core.ui.theme.SvdPrimary
import com.socialvideodownloader.core.ui.theme.SvdSurface
import com.socialvideodownloader.core.ui.theme.SvdTextSecondary
import com.socialvideodownloader.core.ui.theme.SvdTextTertiary
import com.socialvideodownloader.feature.download.R

@Composable
fun ExtractingContent(
    url: String,
    onCancelClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        // URL bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(SvdSurface)
                .border(1.dp, SvdBorder, RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Link,
                contentDescription = null,
                tint = SvdTextTertiary,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = url,
                style = MaterialTheme.typography.bodyMedium,
                color = SvdTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }

        // Spinner section
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            CircularProgressIndicator(
                color = SvdPrimary,
                strokeWidth = 4.dp,
                modifier = Modifier.size(56.dp),
            )
            Text(
                text = stringResource(R.string.download_extracting_status),
                style = MaterialTheme.typography.bodyMedium,
                color = SvdTextSecondary,
            )
        }

        // Cancel button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(AppShapesInstance.cardSm)
                .border(1.5.dp, SvdError, AppShapesInstance.cardSm)
                .clickable(onClick = onCancelClicked),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.download_cancel),
                style = MaterialTheme.typography.labelLarge,
                color = SvdError,
            )
        }
    }
}
