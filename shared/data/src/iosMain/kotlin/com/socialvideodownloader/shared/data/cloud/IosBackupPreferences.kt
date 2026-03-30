package com.socialvideodownloader.shared.data.cloud

import com.russhwolf.settings.Settings
import com.socialvideodownloader.core.domain.sync.BackupPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * iOS implementation of [BackupPreferences] using Multiplatform Settings (NSUserDefaults).
 *
 * Persists cloud backup preferences to NSUserDefaults via the `multiplatform-settings` library.
 * This is the iOS equivalent of Android's DataStore-backed [CloudBackupPreferences].
 *
 * The [Settings] instance is provided by Koin as a singleton, backed by NSUserDefaults.
 * See [IosDataModule] for the binding.
 */
class IosBackupPreferences(
    private val settings: Settings,
) : BackupPreferences {
    companion object {
        private const val KEY_IS_BACKUP_ENABLED = "cloud_backup_is_enabled"
        private const val KEY_LAST_SYNC_TIMESTAMP = "cloud_backup_last_sync_ts"
        private const val KEY_HAS_EVER_ENABLED = "cloud_backup_has_ever_enabled"
    }

    // MutableStateFlows used as hot sources to bridge Settings (not natively reactive) to Flow.
    // Updated every time a setting is written.
    private val _isBackupEnabled =
        MutableStateFlow(
            settings.getBoolean(KEY_IS_BACKUP_ENABLED, defaultValue = false),
        )
    private val _lastSyncTimestamp =
        MutableStateFlow(
            settings.getLong(KEY_LAST_SYNC_TIMESTAMP, defaultValue = 0L),
        )

    override fun observeIsBackupEnabled(): Flow<Boolean> = _isBackupEnabled

    override fun observeLastSyncTimestamp(): Flow<Long> = _lastSyncTimestamp

    override suspend fun hasEverEnabled(): Boolean = settings.getBoolean(KEY_HAS_EVER_ENABLED, defaultValue = false)

    override suspend fun setBackupEnabled(enabled: Boolean) {
        settings.putBoolean(KEY_IS_BACKUP_ENABLED, enabled)
        _isBackupEnabled.value = enabled
    }

    override suspend fun setLastSyncTimestamp(timestamp: Long) {
        settings.putLong(KEY_LAST_SYNC_TIMESTAMP, timestamp)
        _lastSyncTimestamp.value = timestamp
    }

    override suspend fun setHasEverEnabled(hasEver: Boolean) {
        settings.putBoolean(KEY_HAS_EVER_ENABLED, hasEver)
    }
}
