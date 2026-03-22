package com.socialvideodownloader.core.cloud.sync

import android.util.Log
import com.google.firebase.auth.FirebaseAuthException
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import javax.inject.Inject
import kotlin.math.min

private const val TAG = "FirestoreSyncManager"
private const val MAX_RETRIES = 5
private const val BASE_BACKOFF_MS = 1000L
private const val MAX_BACKOFF_MS = 30_000L

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
        try {
            val queue = syncQueueDao.getAll()
            if (queue.isEmpty()) return@withContext

            _syncStatus.value = SyncStatus.Syncing

            var anyFailed = false

            for (op in queue) {
                val backoffMs = min(BASE_BACKOFF_MS * (1L shl op.retryCount), MAX_BACKOFF_MS)
                if (op.retryCount > 0) {
                    delay(backoffMs)
                }

                runCatching {
                    when (op.operation) {
                        "UPLOAD" -> processUploadWithReauth(op)
                        "DELETE" -> processDelete(op)
                    }
                }.onFailure { error ->
                    Log.w(TAG, "Operation ${op.operation} id=${op.id} failed: ${error.message}", error)
                    anyFailed = true
                    syncQueueDao.updateRetry(
                        id = op.id,
                        retryCount = op.retryCount + 1,
                        lastError = error.message,
                    )
                }
            }

            syncQueueDao.deleteFailedOperations(maxRetries = MAX_RETRIES)

            if (anyFailed) {
                _syncStatus.value = SyncStatus.Paused("Backup paused")
            } else {
                val timestamp = System.currentTimeMillis()
                _syncStatus.value = SyncStatus.Synced(lastSyncTimestamp = timestamp)
                backupPreferences.setLastSyncTimestamp(timestamp)
            }
        } catch (e: Exception) {
            Log.e(TAG, "processPendingOperations failed: ${e.message}", e)
            _syncStatus.value = SyncStatus.Paused("Backup paused")
        }
    }

    override suspend fun syncNewRecord(record: DownloadRecord) = withContext(ioDispatcher) {
        try {
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
        } catch (e: Exception) {
            Log.e(TAG, "syncNewRecord failed: ${e.message}", e)
            _syncStatus.value = SyncStatus.Paused("Backup paused")
        }
    }

    override suspend fun queueDeletion(record: DownloadRecord): Unit = withContext(ioDispatcher) {
        try {
            syncQueueDao.insert(
                SyncQueueEntity(
                    downloadId = record.id,
                    operation = "DELETE",
                    createdAt = System.currentTimeMillis(),
                ),
            )
        } catch (e: Exception) {
            Log.e(TAG, "queueDeletion failed: ${e.message}", e)
            // Queue insertion failure is non-fatal — the operation will be retried on next sync
        }
    }

    private suspend fun processUploadWithReauth(op: SyncQueueEntity) {
        try {
            processUpload(op)
        } catch (e: FirebaseAuthException) {
            Log.w(TAG, "Auth expired for op ${op.id}, attempting re-auth", e)
            try {
                cloudAuthService.signInAnonymously()
                // Retry once after re-auth
                processUpload(op)
            } catch (reAuthError: Exception) {
                Log.w(TAG, "Re-auth retry also failed for op ${op.id}: ${reAuthError.message}")
                throw reAuthError
            }
        }
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
