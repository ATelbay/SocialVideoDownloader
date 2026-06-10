package com.socialvideodownloader.core.cloud.repository

import com.socialvideodownloader.core.domain.di.IoDispatcher
import com.socialvideodownloader.core.domain.model.DownloadRecord
import com.socialvideodownloader.core.domain.repository.CloudBackupRepository
import com.socialvideodownloader.core.domain.sync.CloudAuthService
import com.socialvideodownloader.core.domain.sync.EncryptionService
import com.socialvideodownloader.core.domain.sync.cloudDocumentId
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

private const val DEFAULT_TIER_LIMIT = 1000

/**
 * Orchestrates cloud backup on top of a [CloudHistoryDataSource] (raw store I/O) and an
 * [EncryptionService]. All the logic worth testing lives here; the Firestore SDK is kept behind
 * the data-source seam so this class is exercised by [FirestoreCloudBackupRepositoryTest].
 */
class FirestoreCloudBackupRepository
    @Inject
    constructor(
        private val authService: CloudAuthService,
        private val encryptionService: EncryptionService,
        private val dataSource: CloudHistoryDataSource,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : CloudBackupRepository {
        override suspend fun uploadRecord(record: DownloadRecord): Boolean =
            withContext(ioDispatcher) {
                val uid = authService.getCurrentUid() ?: error("Not authenticated — sign in first")
                val hash = cloudDocumentId(record.sourceUrl, record.createdAt)

                // Documents are keyed by a deterministic hash, so writing is idempotent. Only a brand-new
                // document affects capacity and the record counter — re-uploads (retries, record updates,
                // restore) must NOT evict data or inflate the counter.
                val isNewRecord = !dataSource.recordExists(uid, hash)
                if (isNewRecord) {
                    enforceCapacity(uid)
                }

                val encrypted = encryptionService.encrypt(record)
                dataSource.putRecord(
                    uid,
                    CloudRecordDocument(
                        docId = hash,
                        encryptedPayload = encrypted,
                        createdAt = record.createdAt,
                        sourceUrlHash = hash,
                    ),
                )
                if (isNewRecord) {
                    dataSource.adjustRecordCount(uid, 1)
                }
                true
            }

        override suspend fun deleteRecord(sourceUrlHash: String): Boolean =
            withContext(ioDispatcher) {
                val uid = authService.getCurrentUid() ?: return@withContext false
                val deleted = dataSource.deleteRecordsByHash(uid, sourceUrlHash)
                // Decrement by what was actually removed, so deleting a non-existent record never drifts the counter.
                if (deleted > 0) {
                    dataSource.adjustRecordCount(uid, -deleted)
                }
                true
            }

        override suspend fun fetchAllRecords(): List<DownloadRecord> =
            withContext(ioDispatcher) {
                val uid = authService.getCurrentUid() ?: return@withContext emptyList()
                // Decryption + JSON parsing of up to the tier limit (e.g. 1000) records is CPU-bound;
                // keep it off the caller's dispatcher (restore runs on the UI scope on Android).
                dataSource.getAllRecords(uid).mapNotNull { doc ->
                    runCatching { encryptionService.decrypt(doc.encryptedPayload) }.getOrNull()
                }
            }

        override suspend fun getCloudRecordCount(): Int =
            withContext(ioDispatcher) {
                val uid = authService.getCurrentUid() ?: return@withContext 0
                dataSource.getRecordCount(uid)
            }

        override suspend fun getTierLimit(): Int =
            withContext(ioDispatcher) {
                val uid = authService.getCurrentUid() ?: return@withContext DEFAULT_TIER_LIMIT
                dataSource.getTierLimit(uid) ?: DEFAULT_TIER_LIMIT
            }

        override suspend fun updateTierLimit(limit: Int): Unit =
            withContext(ioDispatcher) {
                val uid = authService.getCurrentUid() ?: return@withContext
                dataSource.setTierLimit(uid, limit)
            }

        override suspend fun evictOldestRecords(count: Int): Unit =
            withContext(ioDispatcher) {
                val uid = authService.getCurrentUid() ?: return@withContext
                evictOldest(uid, count)
            }

        override suspend fun setRecordCount(count: Int): Unit =
            withContext(ioDispatcher) {
                val uid = authService.getCurrentUid() ?: return@withContext
                dataSource.setRecordCount(uid, count)
            }

        /** Enforce the tier limit before adding one new record, evicting just enough to make room. */
        private suspend fun enforceCapacity(uid: String) {
            val count = dataSource.getRecordCount(uid)
            val limit = dataSource.getTierLimit(uid) ?: DEFAULT_TIER_LIMIT
            if (count >= limit) {
                evictOldest(uid, count - limit + 1)
            }
        }

        private suspend fun evictOldest(
            uid: String,
            count: Int,
        ) {
            if (count <= 0) return
            val oldest = dataSource.getOldestRecords(uid, count)
            if (oldest.isEmpty()) return
            dataSource.deleteRecordsByIds(uid, oldest.map { it.docId })
            dataSource.adjustRecordCount(uid, -oldest.size)
        }
    }
