package com.socialvideodownloader.shared.data.cloud

import com.socialvideodownloader.core.domain.model.DownloadRecord
import com.socialvideodownloader.core.domain.model.SyncStatus
import com.socialvideodownloader.core.domain.repository.CloudBackupRepository
import com.socialvideodownloader.core.domain.sync.BackupPreferences
import com.socialvideodownloader.core.domain.sync.CloudAuthService
import com.socialvideodownloader.core.domain.sync.SyncManager
import com.socialvideodownloader.shared.data.local.DownloadDao
import com.socialvideodownloader.shared.data.local.SyncQueueDao
import com.socialvideodownloader.shared.data.local.SyncQueueEntity
import com.socialvideodownloader.shared.data.local.toDomain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import platform.Foundation.NSDate
import platform.Foundation.NSTimeIntervalSince1970

/**
 * iOS implementation of [SyncManager].
 *
 * Processes upload and delete operations from the [SyncQueueDao] against the
 * [CloudBackupRepository]. Monitors network connectivity via [IosConnectivityObserver]
 * and pauses sync when the device is offline.
 *
 * Architecture:
 *   - Maintains a local sync queue in the shared Room KMP database (SyncQueueEntity)
 *   - Processes queue items when [processPendingOperations] is called
 *   - Pauses automatically when offline; resumes when connectivity is restored
 *   - Exposes [observeSyncStatus] for real-time UI feedback
 *
 * Note: Unlike Android's SyncManager which integrates with WorkManager, the iOS
 * implementation processes sync directly on the calling coroutine. Background sync
 * is handled by iOS Background App Refresh if configured.
 */
class IosSyncManager(
    private val syncQueueDao: SyncQueueDao,
    private val downloadDao: DownloadDao,
    private val cloudBackupRepository: CloudBackupRepository,
    private val cloudAuthService: CloudAuthService,
    private val backupPreferences: BackupPreferences,
    private val connectivityObserver: IosConnectivityObserver,
) : SyncManager {
    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    private var isOnline = true

    init {
        // Monitor connectivity and trigger pending operations when going back online.
        connectivityObserver.observeConnectivity()
            .distinctUntilChanged()
            .onEach { online ->
                isOnline = online
                if (online) {
                    val isBackupEnabled =
                        runCatching {
                            var enabled = false
                            backupPreferences.observeIsBackupEnabled()
                                .onEach { enabled = it }
                                .launchIn(managerScope)
                            enabled
                        }.getOrDefault(false)

                    if (isBackupEnabled) {
                        managerScope.launch { processPendingOperations() }
                    }
                } else {
                    _syncStatus.value = SyncStatus.Paused("No network connection")
                }
            }
            .launchIn(managerScope)
    }

    /**
     * Process all pending sync operations from the queue.
     *
     * Skips processing if:
     *   - The user is not authenticated
     *   - The device is offline
     *
     * Each item is processed in order. Failed items have their retry count incremented.
     * Items that exceed the max retry count (3) are removed from the queue.
     */
    override suspend fun processPendingOperations() {
        if (!cloudAuthService.isAuthenticated()) return
        if (!isOnline) {
            _syncStatus.value = SyncStatus.Paused("No network connection")
            return
        }

        val pendingItems = syncQueueDao.getAll()
        if (pendingItems.isEmpty()) {
            val timestamp = currentTimeMillis()
            _syncStatus.value = SyncStatus.Synced(timestamp)
            backupPreferences.setLastSyncTimestamp(timestamp)
            return
        }

        _syncStatus.value = SyncStatus.Syncing

        var lastError: String? = null
        for (item in pendingItems) {
            try {
                val success =
                    when (item.operation) {
                        "UPLOAD" -> processUpload(item)
                        "DELETE" -> processDelete(item)
                        else -> {
                            // Unknown operation — remove from queue to prevent infinite loops
                            syncQueueDao.deleteById(item.id)
                            true
                        }
                    }
                if (success) {
                    syncQueueDao.deleteById(item.id)
                }
            } catch (e: Exception) {
                lastError = e.message ?: "Unknown error"
                val newRetryCount = item.retryCount + 1
                if (newRetryCount >= MAX_RETRY_COUNT) {
                    // Give up on this item after max retries
                    syncQueueDao.deleteById(item.id)
                } else {
                    syncQueueDao.updateRetry(item.id, newRetryCount, lastError)
                }
            }
        }

        val timestamp = currentTimeMillis()
        _syncStatus.value =
            if (lastError != null) {
                SyncStatus.Error(lastError)
            } else {
                SyncStatus.Synced(timestamp)
            }
        if (lastError == null) {
            backupPreferences.setLastSyncTimestamp(timestamp)
        }
    }

    /**
     * Queue a newly completed download record for cloud upload.
     *
     * Adds an UPLOAD entry to the sync queue and immediately attempts processing
     * if the device is online and the user is authenticated.
     */
    override suspend fun syncNewRecord(record: DownloadRecord) {
        if (!cloudAuthService.isAuthenticated()) return

        // Add to queue — the unique index on (downloadId, operation) prevents duplicates
        val queueItem =
            SyncQueueEntity(
                downloadId = record.id,
                operation = "UPLOAD",
                createdAt = currentTimeMillis(),
            )
        syncQueueDao.insert(queueItem)

        // Attempt immediate sync if online
        if (isOnline) {
            managerScope.launch { processPendingOperations() }
        }
    }

    /**
     * Queue a deletion for a locally deleted record.
     *
     * Adds a DELETE entry to the sync queue. Any pending UPLOAD for the same
     * download is removed first (no point uploading something being deleted).
     */
    override suspend fun queueDeletion(record: DownloadRecord) {
        if (!cloudAuthService.isAuthenticated()) return

        // Cancel any pending upload for this record by removing from queue
        // (SyncQueueDao does not have a deleteByDownloadIdAndOperation — handle via getAll + filter)
        val existing = syncQueueDao.getAll()
        val pendingUpload = existing.find { it.downloadId == record.id && it.operation == "UPLOAD" }
        if (pendingUpload != null) {
            syncQueueDao.deleteById(pendingUpload.id)
        }

        // Queue the deletion
        val queueItem =
            SyncQueueEntity(
                downloadId = record.id,
                operation = "DELETE",
                createdAt = currentTimeMillis(),
            )
        syncQueueDao.insert(queueItem)
    }

    override fun observeSyncStatus(): Flow<SyncStatus> = _syncStatus.asStateFlow()

    // ---------------------------------------------------------------------------
    // Private helpers
    // ---------------------------------------------------------------------------

    private suspend fun processUpload(item: SyncQueueEntity): Boolean {
        // The record may have been deleted locally before its upload was processed. There is nothing
        // to upload, so drop the stale queue item (return true) instead of looping on it forever — a
        // missing-record `false` would never bump the retry count and would never clear.
        val entity = downloadDao.getById(item.downloadId) ?: return true
        // Capacity enforcement (tier limit + LRU eviction) and counter accounting live inside the
        // repository's uploadRecord, so every upload path goes through the same invariants.
        return cloudBackupRepository.uploadRecord(entity.toDomain())
    }

    private suspend fun processDelete(item: SyncQueueEntity): Boolean {
        // The cloud document id is derived from sourceUrl + createdAt, so we need the original record
        // to recompute it. If the local record is already gone we cannot derive the id; drop the stale
        // queue item rather than retrying an operation that can never succeed.
        val entity = downloadDao.getById(item.downloadId) ?: return true
        // Must match the id used at upload time (CloudBackupRepositoryImpl.uploadRecord) so the right
        // document is targeted.
        val docId = cloudDocumentId(entity.sourceUrl, entity.createdAt)
        return cloudBackupRepository.deleteRecord(docId)
    }

    companion object {
        private const val MAX_RETRY_COUNT = 3
    }
}

/** iOS-compatible replacement for JVM System.currentTimeMillis(). */
private fun currentTimeMillis(): Long = ((NSDate().timeIntervalSinceReferenceDate + NSTimeIntervalSince1970) * 1000).toLong()
