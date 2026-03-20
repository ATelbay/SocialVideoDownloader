package com.socialvideodownloader.core.cloud.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.socialvideodownloader.core.domain.sync.BackupPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "cloud_backup_prefs")

class CloudBackupPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) : BackupPreferences {

    private object Keys {
        val IS_BACKUP_ENABLED = booleanPreferencesKey("isBackupEnabled")
        val LAST_SYNC_TIMESTAMP = longPreferencesKey("lastSyncTimestamp")
        val HAS_EVER_ENABLED = booleanPreferencesKey("hasEverEnabled")
    }

    override fun observeIsBackupEnabled(): Flow<Boolean> =
        context.dataStore.data.map { prefs -> prefs[Keys.IS_BACKUP_ENABLED] ?: false }

    override fun observeLastSyncTimestamp(): Flow<Long> =
        context.dataStore.data.map { prefs -> prefs[Keys.LAST_SYNC_TIMESTAMP] ?: 0L }

    override suspend fun hasEverEnabled(): Boolean =
        context.dataStore.data.first()[Keys.HAS_EVER_ENABLED] ?: false

    override suspend fun setBackupEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.IS_BACKUP_ENABLED] = enabled }
    }

    override suspend fun setLastSyncTimestamp(timestamp: Long) {
        context.dataStore.edit { prefs -> prefs[Keys.LAST_SYNC_TIMESTAMP] = timestamp }
    }

    override suspend fun setHasEverEnabled(hasEver: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.HAS_EVER_ENABLED] = hasEver }
    }
}
