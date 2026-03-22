package com.socialvideodownloader.feature.history.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.socialvideodownloader.feature.history.R

@Composable
fun RestoreDialog(
    restoreState: RestoreState,
    onDismiss: () -> Unit,
) {
    when (restoreState) {
        is RestoreState.Idle -> Unit
        is RestoreState.InProgress -> {
            AlertDialog(
                onDismissRequest = { /* not dismissible during progress */ },
                text = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (restoreState.total > 0) {
                            Text(
                                text = stringResource(
                                    R.string.cloud_restore_progress,
                                    restoreState.current,
                                    restoreState.total,
                                ),
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        } else {
                            CircularProgressIndicator()
                        }
                    }
                },
                confirmButton = {},
            )
        }
        is RestoreState.Completed -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                text = {
                    Text(
                        text = stringResource(
                            R.string.cloud_restore_complete,
                            restoreState.restored,
                            restoreState.skipped,
                        ),
                    )
                },
                confirmButton = {
                    TextButton(onClick = onDismiss) {
                        Text(text = stringResource(android.R.string.ok))
                    }
                },
            )
        }
        is RestoreState.Error -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                text = {
                    Text(
                        text = if (restoreState.message.contains("key", ignoreCase = true)) {
                            stringResource(R.string.cloud_restore_key_lost)
                        } else {
                            restoreState.message
                        },
                    )
                },
                confirmButton = {
                    TextButton(onClick = onDismiss) {
                        Text(text = stringResource(android.R.string.ok))
                    }
                },
            )
        }
    }
}
