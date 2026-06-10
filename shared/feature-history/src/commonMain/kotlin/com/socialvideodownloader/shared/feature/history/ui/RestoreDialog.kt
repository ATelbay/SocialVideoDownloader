package com.socialvideodownloader.shared.feature.history.ui

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
import androidx.compose.ui.unit.dp
import com.socialvideodownloader.core.domain.sync.RestoreErrorReason
import com.socialvideodownloader.shared.feature.history.RestoreState
import com.socialvideodownloader.shared.ui.theme.SvdPrimary

@Composable
fun RestoreDialog(
    restoreState: RestoreState,
    okLabel: String,
    progressText: (current: Int, total: Int) -> String,
    completedText: (restored: Int, skipped: Int) -> String,
    keyLostText: String,
    genericErrorText: String,
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
                                text = progressText(restoreState.current, restoreState.total),
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        } else {
                            CircularProgressIndicator(color = SvdPrimary)
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
                        text = completedText(restoreState.restored, restoreState.skipped),
                    )
                },
                confirmButton = {
                    TextButton(onClick = onDismiss) {
                        Text(text = okLabel)
                    }
                },
            )
        }
        is RestoreState.Error -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                text = {
                    Text(
                        text =
                            when (restoreState.reason) {
                                RestoreErrorReason.KEY_UNAVAILABLE -> keyLostText
                                RestoreErrorReason.GENERIC -> genericErrorText
                            },
                    )
                },
                confirmButton = {
                    TextButton(onClick = onDismiss) {
                        Text(text = okLabel)
                    }
                },
            )
        }
    }
}
