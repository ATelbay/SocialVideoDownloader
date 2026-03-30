package com.socialvideodownloader.core.domain.fake

import com.socialvideodownloader.core.domain.sync.BackupPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeBackupPreferences : BackupPreferences {
    private val backupEnabled = MutableStateFlow(false)
    private val lastSyncTimestamp = MutableStateFlow(0L)
    private var hasEver = false

    var setBackupEnabledCalls = mutableListOf<Boolean>()
    var setHasEverEnabledCalls = mutableListOf<Boolean>()

    override fun observeIsBackupEnabled(): Flow<Boolean> = backupEnabled

    override fun observeLastSyncTimestamp(): Flow<Long> = lastSyncTimestamp

    override suspend fun hasEverEnabled(): Boolean = hasEver

    override suspend fun setBackupEnabled(enabled: Boolean) {
        setBackupEnabledCalls.add(enabled)
        backupEnabled.value = enabled
    }

    override suspend fun setLastSyncTimestamp(timestamp: Long) {
        lastSyncTimestamp.value = timestamp
    }

    override suspend fun setHasEverEnabled(hasEver: Boolean) {
        setHasEverEnabledCalls.add(hasEver)
        this.hasEver = hasEver
    }

    fun setHasEverEnabledState(value: Boolean) {
        hasEver = value
    }
}
