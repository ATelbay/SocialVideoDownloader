package com.socialvideodownloader.shared.data.cloud

import com.socialvideodownloader.core.domain.model.DownloadRecord
import com.socialvideodownloader.core.domain.repository.CloudBackupRepository
import com.socialvideodownloader.core.domain.sync.BackupPreferences
import com.socialvideodownloader.core.domain.sync.CloudAuthService
import com.socialvideodownloader.shared.data.local.DownloadDao
import com.socialvideodownloader.shared.data.local.DownloadEntity
import com.socialvideodownloader.shared.data.local.SyncQueueDao
import com.socialvideodownloader.shared.data.local.SyncQueueEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Verifies the [DownloadDao] wiring added to [IosSyncManager]: that UPLOAD/DELETE queue items resolve
 * their record through the DAO before touching the cloud, and that the document id used for deletion
 * matches the one produced at upload time ([cloudDocumentId]).
 *
 * Lives in `iosTest` because [IosSyncManager] is iOS-only. Uses hand-written fakes (MockK is JVM-only).
 * Construction reads `observeIsBackupEnabled` synchronously (always false at init), so the connectivity
 * watcher never auto-triggers sync — every test drives [IosSyncManager.processPendingOperations] itself.
 */
class IosSyncManagerTest {
    private fun manager(
        syncQueueDao: SyncQueueDao,
        downloadDao: DownloadDao,
        cloudBackupRepository: CloudBackupRepository,
    ): IosSyncManager =
        IosSyncManager(
            syncQueueDao = syncQueueDao,
            downloadDao = downloadDao,
            cloudBackupRepository = cloudBackupRepository,
            cloudAuthService = FakeCloudAuthService(authenticated = true),
            backupPreferences = FakeBackupPreferences(),
            connectivityObserver = IosConnectivityObserver(StubConnectivityProvider()),
        )

    @Test
    fun processUploadLoadsRecordFromDownloadDaoAndUploads() =
        runTest {
            val downloadDao = FakeDownloadDao().apply { put(entity(id = 1, sourceUrl = "https://example.com/v1", createdAt = 1000L)) }
            val queue = FakeSyncQueueDao().apply { seed(SyncQueueEntity(downloadId = 1, operation = "UPLOAD", createdAt = 0L)) }
            val repo = RecordingCloudBackupRepository()

            manager(queue, downloadDao, repo).processPendingOperations()

            assertEquals(listOf("https://example.com/v1"), repo.uploaded.map { it.sourceUrl })
            assertTrue(queue.getAll().isEmpty(), "successful upload should clear the queue item")
        }

    @Test
    fun processUploadDropsStaleQueueItemWhenRecordMissing() =
        runTest {
            val queue = FakeSyncQueueDao().apply { seed(SyncQueueEntity(downloadId = 99, operation = "UPLOAD", createdAt = 0L)) }
            val repo = RecordingCloudBackupRepository()

            manager(queue, FakeDownloadDao(), repo).processPendingOperations()

            assertTrue(repo.uploaded.isEmpty(), "missing record must not upload anything")
            assertTrue(queue.getAll().isEmpty(), "stale upload item must be dropped, not retried forever")
        }

    @Test
    fun processDeleteUsesCloudDocumentIdDerivedFromRecord() =
        runTest {
            val downloadDao = FakeDownloadDao().apply { put(entity(id = 1, sourceUrl = "https://example.com/v1", createdAt = 1000L)) }
            val queue = FakeSyncQueueDao().apply { seed(SyncQueueEntity(downloadId = 1, operation = "DELETE", createdAt = 0L)) }
            val repo = RecordingCloudBackupRepository()

            manager(queue, downloadDao, repo).processPendingOperations()

            assertEquals(listOf(cloudDocumentId("https://example.com/v1", 1000L)), repo.deleted)
            assertTrue(queue.getAll().isEmpty(), "successful delete should clear the queue item")
        }

    @Test
    fun processDeleteDropsStaleQueueItemWhenRecordMissing() =
        runTest {
            val queue = FakeSyncQueueDao().apply { seed(SyncQueueEntity(downloadId = 99, operation = "DELETE", createdAt = 0L)) }
            val repo = RecordingCloudBackupRepository()

            manager(queue, FakeDownloadDao(), repo).processPendingOperations()

            assertTrue(repo.deleted.isEmpty(), "missing record can't derive a document id, so nothing is deleted")
            assertTrue(queue.getAll().isEmpty(), "stale delete item must be dropped, not retried forever")
        }

    private fun entity(
        id: Long,
        sourceUrl: String,
        createdAt: Long,
    ) = DownloadEntity(
        id = id,
        sourceUrl = sourceUrl,
        videoTitle = "Title $id",
        thumbnailUrl = null,
        filePath = "/tmp/$id.mp4",
        status = "COMPLETED",
        createdAt = createdAt,
        completedAt = createdAt,
        fileSizeBytes = 1L,
    )
}

private class RecordingCloudBackupRepository : CloudBackupRepository {
    val uploaded = mutableListOf<DownloadRecord>()
    val deleted = mutableListOf<String>()

    override suspend fun uploadRecord(record: DownloadRecord): Boolean {
        uploaded += record
        return true
    }

    override suspend fun deleteRecord(sourceUrlHash: String): Boolean {
        deleted += sourceUrlHash
        return true
    }

    override suspend fun fetchAllRecords(): List<DownloadRecord> = emptyList()

    override suspend fun getCloudRecordCount(): Int = 0

    override suspend fun getTierLimit(): Int = 1000

    override suspend fun updateTierLimit(limit: Int) = Unit

    override suspend fun evictOldestRecords(count: Int) = Unit

    override suspend fun setRecordCount(count: Int) = Unit
}

/** In-memory [SyncQueueDao] with auto-increment ids, mirroring the unique (downloadId, operation) index. */
private class FakeSyncQueueDao : SyncQueueDao {
    private val items = mutableListOf<SyncQueueEntity>()
    private var nextId = 1L

    fun seed(entity: SyncQueueEntity) {
        items += entity.copy(id = nextId++)
    }

    override suspend fun getAll(): List<SyncQueueEntity> = items.sortedBy { it.createdAt }

    override suspend fun insert(entity: SyncQueueEntity) {
        val exists = items.any { it.downloadId == entity.downloadId && it.operation == entity.operation }
        if (!exists) items += entity.copy(id = nextId++)
    }

    override suspend fun deleteById(id: Long) {
        items.removeAll { it.id == id }
    }

    override suspend fun updateRetry(
        id: Long,
        retryCount: Int,
        lastError: String?,
    ) {
        val idx = items.indexOfFirst { it.id == id }
        if (idx >= 0) items[idx] = items[idx].copy(retryCount = retryCount, lastError = lastError)
    }

    override suspend fun deleteFailedOperations(maxRetries: Int) {
        items.removeAll { it.retryCount >= maxRetries }
    }
}

/** In-memory [DownloadDao]; only the lookups [IosSyncManager] uses are meaningful, the rest are no-ops. */
private class FakeDownloadDao : DownloadDao {
    private val rows = mutableMapOf<Long, DownloadEntity>()

    fun put(entity: DownloadEntity) {
        rows[entity.id] = entity
    }

    override fun getAll(): Flow<List<DownloadEntity>> = flowOf(rows.values.toList())

    override suspend fun getById(id: Long): DownloadEntity? = rows[id]

    override suspend fun insert(entity: DownloadEntity): Long {
        rows[entity.id] = entity
        return entity.id
    }

    override suspend fun update(entity: DownloadEntity) {
        rows[entity.id] = entity
    }

    override suspend fun delete(entity: DownloadEntity) {
        rows.remove(entity.id)
    }

    override suspend fun deleteAll() {
        rows.clear()
    }

    override fun getCompleted(): Flow<List<DownloadEntity>> = emptyFlow()

    override suspend fun getCompletedSnapshot(): List<DownloadEntity> = rows.values.toList()
}

private class FakeCloudAuthService(private val authenticated: Boolean) : CloudAuthService {
    override suspend fun signInWithGoogleCredential(idToken: String): String = "uid"

    override fun getCurrentUid(): String? = if (authenticated) "uid" else null

    override fun isAuthenticated(): Boolean = authenticated

    override suspend fun signOut() = Unit

    override fun getDisplayName(): String? = null

    override fun getPhotoUrl(): String? = null
}

private class FakeBackupPreferences : BackupPreferences {
    override fun observeIsBackupEnabled(): Flow<Boolean> = MutableStateFlow(true)

    override fun observeLastSyncTimestamp(): Flow<Long> = MutableStateFlow(0L)

    override suspend fun hasEverEnabled(): Boolean = true

    override suspend fun setBackupEnabled(enabled: Boolean) = Unit

    override suspend fun setLastSyncTimestamp(timestamp: Long) = Unit

    override suspend fun setHasEverEnabled(hasEver: Boolean) = Unit
}
