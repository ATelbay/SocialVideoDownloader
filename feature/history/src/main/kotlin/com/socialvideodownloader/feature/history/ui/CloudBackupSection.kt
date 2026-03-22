package com.socialvideodownloader.feature.history.ui

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.socialvideodownloader.core.domain.model.SyncStatus
import com.socialvideodownloader.feature.history.R
import java.text.DateFormat
import java.util.Date

@Composable
fun CloudBackupSection(
    state: CloudBackupState,
    onToggleBackup: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (!state.isSignedIn) {
            // Not signed in: show sign-in button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.cloud_backup_toggle_label),
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (state.isSigningIn) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Button(onClick = onToggleBackup) {
                        Text(text = stringResource(R.string.cloud_sign_in_google))
                    }
                }
            }
            if (state.signInError != null) {
                Text(
                    text = stringResource(R.string.cloud_sign_in_failed),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        } else {
            // Signed in: show user info + backup toggle + sign out
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (state.userPhotoUrl != null) {
                    AsyncImage(
                        model = state.userPhotoUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = state.userName
                        ?: stringResource(R.string.cloud_signed_in_as, ""),
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
                    text = stringResource(R.string.cloud_backup_toggle_label),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Switch(
                    checked = state.isCloudBackupEnabled,
                    onCheckedChange = { onToggleBackup() },
                )
            }
            val statusText = syncStatusText(state.isCloudBackupEnabled, state.syncStatus)
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
                Text(text = stringResource(R.string.cloud_sign_out))
            }
        }
    }
}

@Composable
private fun syncStatusText(isEnabled: Boolean, syncStatus: SyncStatus): String? {
    if (!isEnabled) return stringResource(R.string.cloud_backup_disabled)
    return when (syncStatus) {
        is SyncStatus.Idle -> stringResource(R.string.cloud_backup_never)
        is SyncStatus.Syncing -> stringResource(R.string.cloud_backup_syncing)
        is SyncStatus.Synced -> {
            val formattedTime = DateFormat.getTimeInstance(DateFormat.SHORT)
                .format(Date(syncStatus.lastSyncTimestamp))
            stringResource(R.string.cloud_backup_synced, formattedTime)
        }
        is SyncStatus.Paused -> stringResource(R.string.cloud_backup_paused)
        is SyncStatus.Error -> stringResource(R.string.cloud_backup_error)
    }
}
