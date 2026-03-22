package com.socialvideodownloader.core.domain.model

sealed interface SyncStatus {
    data object Idle : SyncStatus

    data object Syncing : SyncStatus

    data class Synced(val lastSyncTimestamp: Long) : SyncStatus

    data class Paused(val reason: String) : SyncStatus

    data class Error(val message: String) : SyncStatus
}
