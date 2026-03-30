package com.socialvideodownloader.core.domain.fake

import com.socialvideodownloader.core.domain.model.DownloadRecord
import com.socialvideodownloader.core.domain.model.SyncStatus
import com.socialvideodownloader.core.domain.sync.SyncManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSyncManager : SyncManager {
    var syncedRecords = mutableListOf<DownloadRecord>()
    var deletionQueue = mutableListOf<DownloadRecord>()
    var processPendingCalled = false
    private val syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)

    override suspend fun processPendingOperations() {
        processPendingCalled = true
    }

    override suspend fun syncNewRecord(record: DownloadRecord) {
        syncedRecords.add(record)
    }

    override suspend fun queueDeletion(record: DownloadRecord) {
        deletionQueue.add(record)
    }

    override fun observeSyncStatus(): Flow<SyncStatus> = syncStatus
}
