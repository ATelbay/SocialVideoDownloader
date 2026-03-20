package com.socialvideodownloader.core.cloud.sync

import com.socialvideodownloader.core.data.local.DownloadDao
import com.socialvideodownloader.core.data.local.SyncQueueDao
import com.socialvideodownloader.core.data.local.SyncQueueEntity
import com.socialvideodownloader.core.data.local.toDomain
import com.socialvideodownloader.core.domain.di.IoDispatcher
import com.socialvideodownloader.core.domain.model.DownloadRecord
import com.socialvideodownloader.core.domain.model.SyncStatus
import com.socialvideodownloader.core.domain.repository.CloudBackupRepository
import com.socialvideodownloader.core.domain.sync.BackupPreferences
import com.socialvideodownloader.core.domain.sync.CloudAuthService
import com.socialvideodownloader.core.domain.sync.EncryptionService
import com.socialvideodownloader.core.domain.sync.SyncManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import javax.inject.Inject

class FirestoreSyncManager @Inject constructor(
    private val syncQueueDao: SyncQueueDao,
    private val downloadDao: DownloadDao,
    private val cloudBackupRepository: CloudBackupRepository,
    private val encryptionService: EncryptionService,
    private val backupPreferences: BackupPreferences,
    private val cloudAuthService: CloudAuthService,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : SyncManager {

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)

    override fun observeSyncStatus(): Flow<SyncStatus> = _syncStatus.asStateFlow()

    override suspend fun processPendingOperations() = withContext(ioDispatcher) {
        val queue = syncQueueDao.getAll()
        if (queue.isEmpty()) return@withContext

        _syncStatus.value = SyncStatus.Syncing

        for (op in queue) {
            runCatching {
                when (op.operation) {
                    "UPLOAD" -> processUpload(op)
                    "DELETE" -> processDelete(op)
                }
            }.onFailure { error ->
                syncQueueDao.updateRetry(
                    id = op.id,
                    retryCount = op.retryCount + 1,
                    lastError = error.message,
                )
            }
        }

        syncQueueDao.deleteFailedOperations(maxRetries = 5)
        val timestamp = System.currentTimeMillis()
        _syncStatus.value = SyncStatus.Synced(lastSyncTimestamp = timestamp)
        backupPreferences.setLastSyncTimestamp(timestamp)
    }

    override suspend fun syncNewRecord(record: DownloadRecord) = withContext(ioDispatcher) {
        _syncStatus.value = SyncStatus.Syncing
        val currentCount = cloudBackupRepository.getCloudRecordCount()
        val tierLimit = cloudBackupRepository.getTierLimit()
        if (currentCount >= tierLimit) {
            cloudBackupRepository.evictOldestRecords(1)
        }
        val success = cloudBackupRepository.uploadRecord(record)
        if (success) {
            val timestamp = System.currentTimeMillis()
            _syncStatus.value = SyncStatus.Synced(lastSyncTimestamp = timestamp)
            backupPreferences.setLastSyncTimestamp(timestamp)
        } else {
            _syncStatus.value = SyncStatus.Error("Upload failed")
        }
    }

    override suspend fun queueDeletion(record: DownloadRecord) = withContext(ioDispatcher) {
        syncQueueDao.insert(
            SyncQueueEntity(
                downloadId = record.id,
                operation = "DELETE",
                createdAt = System.currentTimeMillis(),
            ),
        )
    }

    private suspend fun processUpload(op: SyncQueueEntity) {
        val entity = downloadDao.getById(op.downloadId) ?: return
        val record = entity.toDomain()
        val success = cloudBackupRepository.uploadRecord(record)
        if (success) {
            syncQueueDao.deleteById(op.id)
        }
    }

    private suspend fun processDelete(op: SyncQueueEntity) {
        val entity = downloadDao.getById(op.downloadId)
        val sourceUrl = entity?.sourceUrl ?: return
        val createdAt = entity.createdAt
        val hash = sourceUrlHash(sourceUrl, createdAt)
        val success = cloudBackupRepository.deleteRecord(hash)
        if (success) {
            syncQueueDao.deleteById(op.id)
        }
    }

    private fun sourceUrlHash(sourceUrl: String, createdAt: Long): String {
        val input = "$sourceUrl$createdAt"
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(input.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
}
