package com.socialvideodownloader.core.domain.sync

import kotlinx.coroutines.flow.Flow

interface BackupPreferences {
    fun observeIsBackupEnabled(): Flow<Boolean>
    fun observeLastSyncTimestamp(): Flow<Long>
    suspend fun hasEverEnabled(): Boolean
    suspend fun setBackupEnabled(enabled: Boolean)
    suspend fun setLastSyncTimestamp(timestamp: Long)
    suspend fun setHasEverEnabled(hasEver: Boolean)
}
