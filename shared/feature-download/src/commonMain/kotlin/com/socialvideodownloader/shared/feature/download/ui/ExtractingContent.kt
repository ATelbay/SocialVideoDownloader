package com.socialvideodownloader.shared.feature.download.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.socialvideodownloader.shared.ui.theme.LocalAppShapes
import com.socialvideodownloader.shared.ui.theme.Spacing
import com.socialvideodownloader.shared.ui.theme.SvdBorder
import com.socialvideodownloader.shared.ui.theme.SvdBorderStrong
import com.socialvideodownloader.shared.ui.theme.SvdForeground
import com.socialvideodownloader.shared.ui.theme.SvdMutedForeground
import com.socialvideodownloader.shared.ui.theme.SvdPrimary
import com.socialvideodownloader.shared.ui.theme.SvdSubtleForeground
import com.socialvideodownloader.shared.ui.theme.SvdSurface

@Composable
fun ExtractingContent(
    url: String,
    onCancelClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shapes = LocalAppShapes.current
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        // URL bar
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(Spacing.InputHeight)
                    .clip(shapes.control)
                    .background(SvdSurface)
                    .border(1.dp, SvdBorder, shapes.control)
                    .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Link,
                contentDescription = null,
                tint = SvdSubtleForeground,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = url,
                style = MaterialTheme.typography.bodyMedium,
                color = SvdMutedForeground,
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
                text = "Extracting video info\u2026",
                style = MaterialTheme.typography.bodyMedium,
                color = SvdMutedForeground,
            )
        }

        // Cancel button
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = Spacing.SecondaryButtonHeight)
                    .clip(shapes.control)
                    .border(1.dp, SvdBorderStrong, shapes.control)
                    .semantics(mergeDescendants = true) {
                        role = Role.Button
                        contentDescription = "Cancel"
                    }
                    .clickable(onClick = onCancelClicked),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Cancel",
                style = MaterialTheme.typography.labelLarge,
                color = SvdForeground,
            )
        }
    }
}
