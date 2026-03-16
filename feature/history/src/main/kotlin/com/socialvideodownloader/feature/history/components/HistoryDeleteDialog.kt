package com.socialvideodownloader.feature.history.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.socialvideodownloader.core.ui.theme.SvdBorder
import com.socialvideodownloader.core.ui.theme.SvdError
import com.socialvideodownloader.core.ui.theme.SvdErrorContainer
import com.socialvideodownloader.core.ui.theme.SvdPrimary
import com.socialvideodownloader.core.ui.theme.SvdSurfaceElevated
import com.socialvideodownloader.core.ui.theme.SvdText
import com.socialvideodownloader.feature.history.R
import com.socialvideodownloader.feature.history.ui.DeleteConfirmationState
import com.socialvideodownloader.feature.history.ui.DeleteTarget

@Composable
fun HistoryDeleteDialog(
    state: DeleteConfirmationState,
    onDeleteFilesSelectionChanged: (Boolean) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val title = when (state.target) {
        is DeleteTarget.Single -> stringResource(R.string.history_delete_single_title)
        DeleteTarget.All -> stringResource(R.string.history_delete_all_title)
    }

    val bodyText = when (state.target) {
        is DeleteTarget.Single -> stringResource(R.string.history_delete_message_single)
        DeleteTarget.All -> stringResource(R.string.history_delete_message_all, state.affectedCount)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = SvdSurfaceElevated,
            tonalElevation = 6.dp,
            shadowElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp),
            ) {
                // Trash icon in errorContainer circle
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(SvdErrorContainer),
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
                    style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                    color = SvdText,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = bodyText,
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    color = SvdText,
                )

                if (state.hasAnyAccessibleFile) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SvdSurfaceElevated)
                            .clickable { onDeleteFilesSelectionChanged(!state.deleteFilesSelected) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Checkbox(
                            checked = state.deleteFilesSelected,
                            onCheckedChange = onDeleteFilesSelectionChanged,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.history_delete_checkbox_label),
                            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                            color = SvdText,
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
                            text = stringResource(R.string.history_delete_cancel),
                            color = SvdPrimary,
                        )
                    }

                    TextButton(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = stringResource(R.string.history_delete_confirm),
                            color = SvdError,
                        )
                    }
                }
            }
        }
    }
}
