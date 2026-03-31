package com.socialvideodownloader.shared.feature.history.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.socialvideodownloader.shared.feature.history.DeleteConfirmationState
import com.socialvideodownloader.shared.ui.theme.LocalAppShapes
import com.socialvideodownloader.shared.ui.theme.SvdBorder
import com.socialvideodownloader.shared.ui.theme.SvdError
import com.socialvideodownloader.shared.ui.theme.SvdErrorSoft
import com.socialvideodownloader.shared.ui.theme.SvdForeground
import com.socialvideodownloader.shared.ui.theme.SvdPrimaryStrong
import com.socialvideodownloader.shared.ui.theme.SvdSurfaceAlt

@Composable
fun HistoryDeleteDialog(
    state: DeleteConfirmationState,
    title: String,
    bodyText: String,
    deleteFilesLabel: String,
    cancelLabel: String,
    confirmLabel: String,
    onDeleteFilesSelectionChanged: (Boolean) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val shapes = LocalAppShapes.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = shapes.cardLg,
            color = SvdSurfaceAlt,
            tonalElevation = 6.dp,
            shadowElevation = 6.dp,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(SvdErrorSoft),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = null,
                        tint = SvdError,
                        modifier = Modifier.size(28.dp),
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = SvdForeground,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = bodyText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SvdForeground,
                )

                if (state.hasAnyAccessibleFile) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clip(shapes.control)
                                .background(SvdSurfaceAlt)
                                .clickable { onDeleteFilesSelectionChanged(!state.deleteFilesSelected) }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Checkbox(
                            checked = state.deleteFilesSelected,
                            onCheckedChange = null,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = deleteFilesLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = SvdForeground,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                HorizontalDivider(color = SvdBorder)

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = cancelLabel,
                            color = SvdPrimaryStrong,
                        )
                    }

                    TextButton(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = confirmLabel,
                            color = SvdError,
                        )
                    }
                }
            }
        }
    }
}
