package com.socialvideodownloader.shared.feature.history.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.socialvideodownloader.core.domain.model.SyncStatus
import com.socialvideodownloader.shared.feature.history.CloudBackupState

@Composable
fun CloudBackupSection(
    state: CloudBackupState,
    backupToggleLabel: String,
    signInLabel: String,
    signOutLabel: String,
    signedInAs: String,
    signInFailedMessage: String,
    backupDisabledText: String,
    backupNeverText: String,
    backupSyncingText: String,
    backupSyncedText: (time: String) -> String,
    backupPausedText: String,
    backupErrorText: String,
    onToggleBackup: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (!state.isSignedIn) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = backupToggleLabel,
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (state.isSigningIn) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Button(onClick = onToggleBackup) {
                        Text(text = signInLabel)
                    }
                }
            }
            if (state.signInError != null) {
                Text(
                    text = signInFailedMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (state.userPhotoUrl != null) {
                    AsyncImage(
                        model = state.userPhotoUrl,
                        contentDescription = null,
                        modifier =
                            Modifier
                                .size(32.dp)
                                .clip(CircleShape),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = state.userName ?: signedInAs,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = backupToggleLabel,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Switch(
                    checked = state.isCloudBackupEnabled,
                    onCheckedChange = { onToggleBackup() },
                    modifier =
                        Modifier.semantics {
                            contentDescription = backupToggleLabel
                        },
                )
            }
            val statusText =
                syncStatusText(
                    isEnabled = state.isCloudBackupEnabled,
                    syncStatus = state.syncStatus,
                    backupDisabledText = backupDisabledText,
                    backupNeverText = backupNeverText,
                    backupSyncingText = backupSyncingText,
                    backupSyncedText = backupSyncedText,
                    backupPausedText = backupPausedText,
                    backupErrorText = backupErrorText,
                )
            if (statusText != null) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(
                onClick = onSignOut,
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(text = signOutLabel)
            }
        }
    }
}

private fun syncStatusText(
    isEnabled: Boolean,
    syncStatus: SyncStatus,
    backupDisabledText: String,
    backupNeverText: String,
    backupSyncingText: String,
    backupSyncedText: (String) -> String,
    backupPausedText: String,
    backupErrorText: String,
): String? {
    if (!isEnabled) return backupDisabledText
    return when (syncStatus) {
        is SyncStatus.Idle -> backupNeverText
        is SyncStatus.Syncing -> backupSyncingText
        is SyncStatus.Synced -> backupSyncedText(formatTimestamp(syncStatus.lastSyncTimestamp))
        is SyncStatus.Paused -> backupPausedText
        is SyncStatus.Error -> backupErrorText
    }
}

expect fun formatTimestamp(epochMillis: Long): String
