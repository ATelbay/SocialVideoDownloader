package com.socialvideodownloader.core.domain.sync

import com.socialvideodownloader.core.domain.model.DownloadRecord
import com.socialvideodownloader.core.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow

interface SyncManager {
    /** Process all pending sync operations (uploads + deletes). */
    suspend fun processPendingOperations()

    /** Sync a single newly completed download. */
    suspend fun syncNewRecord(record: DownloadRecord)

    /** Queue a cloud deletion for a locally deleted record. */
    suspend fun queueDeletion(record: DownloadRecord)

    /** Observe sync status for UI indicator. */
    fun observeSyncStatus(): Flow<SyncStatus>
}
