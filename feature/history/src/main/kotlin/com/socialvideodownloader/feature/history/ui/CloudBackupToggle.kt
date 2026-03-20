package com.socialvideodownloader.feature.history.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.socialvideodownloader.core.domain.model.SyncStatus
import com.socialvideodownloader.feature.history.R
import java.text.DateFormat
import java.util.Date

@Composable
fun CloudBackupToggle(
    isEnabled: Boolean,
    syncStatus: SyncStatus,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
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
                checked = isEnabled,
                onCheckedChange = { onToggle() },
            )
        }
        val statusText = syncStatusText(isEnabled, syncStatus)
        if (statusText != null) {
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
