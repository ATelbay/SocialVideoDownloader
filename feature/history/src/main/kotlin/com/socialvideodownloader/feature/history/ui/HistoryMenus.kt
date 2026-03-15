package com.socialvideodownloader.feature.history.ui

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.socialvideodownloader.feature.history.R

@Composable
fun HistoryItemMenu(
    isVisible: Boolean,
    isFileAccessible: Boolean,
    onShareClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DropdownMenu(
        expanded = isVisible,
        onDismissRequest = onDismiss,
        modifier = modifier,
    ) {
        if (isFileAccessible) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.history_action_share)) },
                onClick = {
                    onShareClick()
                    onDismiss()
                },
            )
        }
        DropdownMenuItem(
            text = { Text(stringResource(R.string.history_action_delete)) },
            onClick = {
                onDeleteClick()
                onDismiss()
            },
        )
    }
}
