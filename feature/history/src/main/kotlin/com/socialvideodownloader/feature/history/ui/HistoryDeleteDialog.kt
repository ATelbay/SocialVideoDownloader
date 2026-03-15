package com.socialvideodownloader.feature.history.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.socialvideodownloader.feature.history.R

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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            androidx.compose.foundation.layout.Column {
                Text(text = bodyText)
                if (state.hasAnyAccessibleFile) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = state.deleteFilesSelected,
                            onCheckedChange = onDeleteFilesSelectionChanged,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = stringResource(R.string.history_delete_files_checkbox))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(R.string.history_delete_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.history_delete_cancel))
            }
        },
    )
}
