package com.socialvideodownloader.shared.feature.history.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.socialvideodownloader.shared.ui.theme.SvdBorder
import com.socialvideodownloader.shared.ui.theme.SvdError
import com.socialvideodownloader.shared.ui.theme.SvdForeground
import com.socialvideodownloader.shared.ui.theme.SvdSubtleForeground
import com.socialvideodownloader.shared.ui.theme.SvdSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryBottomSheet(
    title: String,
    showShare: Boolean,
    copyLinkLabel: String,
    shareLabel: String,
    deleteLabel: String,
    onCopyLink: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = SvdSurface,
        dragHandle = {
            Box(
                modifier =
                    Modifier
                        .padding(vertical = 12.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .alpha(0.5f)
                        .background(
                            color = SvdSubtleForeground,
                            shape = RoundedCornerShape(2.dp),
                        ),
            )
        },
        modifier = modifier,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = SvdForeground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
            )

            HorizontalDivider(color = SvdBorder)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onCopyLink)
                        .padding(horizontal = 16.dp, vertical = 16.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Link,
                    contentDescription = null,
                    tint = SvdForeground,
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = copyLinkLabel,
                    style = MaterialTheme.typography.bodyLarge,
                    color = SvdForeground,
                )
            }

            HorizontalDivider(color = SvdBorder)

            if (showShare) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onShare)
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = null,
                        tint = SvdForeground,
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = shareLabel,
                        style = MaterialTheme.typography.bodyLarge,
                        color = SvdForeground,
                    )
                }

                HorizontalDivider(color = SvdBorder)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onDelete)
                        .padding(horizontal = 16.dp, vertical = 16.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = null,
                    tint = SvdError,
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = deleteLabel,
                    style = MaterialTheme.typography.bodyLarge,
                    color = SvdError,
                )
            }
        }
    }
}
