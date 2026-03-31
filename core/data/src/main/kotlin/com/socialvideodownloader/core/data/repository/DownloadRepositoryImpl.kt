package com.socialvideodownloader.core.data.repository

import com.socialvideodownloader.core.data.local.DownloadDao
import com.socialvideodownloader.core.data.local.SyncQueueDao
import com.socialvideodownloader.core.data.local.SyncQueueEntity
import com.socialvideodownloader.core.data.local.toDomain
import com.socialvideodownloader.core.data.local.toEntity
import com.socialvideodownloader.core.domain.model.DownloadRecord
import com.socialvideodownloader.core.domain.model.DownloadStatus
import com.socialvideodownloader.core.domain.repository.DownloadRepository
import com.socialvideodownloader.core.domain.sync.BackupPreferences
import com.socialvideodownloader.core.domain.sync.SyncManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DownloadRepositoryImpl
    @Inject
    constructor(
        private val downloadDao: DownloadDao,
        private val syncQueueDao: SyncQueueDao,
        private val backupPreferences: BackupPreferences,
        private val syncManager: dagger.Lazy<SyncManager>,
    ) : DownloadRepository {
        override fun getAll(): Flow<List<DownloadRecord>> = downloadDao.getAll().map { entities -> entities.map { it.toDomain() } }

        override fun getCompletedDownloads(): Flow<List<DownloadRecord>> =
            downloadDao.getCompleted().map { entities -> entities.map { it.toDomain() } }

        override suspend fun getById(id: Long): DownloadRecord? = downloadDao.getById(id)?.toDomain()

        override suspend fun getCompletedSnapshot(): List<DownloadRecord> = downloadDao.getCompletedSnapshot().map { it.toDomain() }

        override suspend fun insert(record: DownloadRecord): Long {
            val id = downloadDao.insert(record.toEntity())
            if (record.status == DownloadStatus.COMPLETED && backupPreferences.observeIsBackupEnabled().first()) {
                syncQueueDao.insert(
                    SyncQueueEntity(
                        downloadId = id,
                        operation = "UPLOAD",
                        createdAt = System.currentTimeMillis(),
                    ),
                )
                syncManager.get().processPendingOperations()
            }
            return id
        }

        override suspend fun update(record: DownloadRecord) {
            downloadDao.update(record.toEntity())
            if (record.status == DownloadStatus.COMPLETED && backupPreferences.observeIsBackupEnabled().first()) {
                syncQueueDao.insert(
                    SyncQueueEntity(
                        downloadId = record.id,
                        operation = "UPLOAD",
                        createdAt = System.currentTimeMillis(),
                    ),
                )
                syncManager.get().processPendingOperations()
            }
        }

        override suspend fun delete(record: DownloadRecord) {
            if (record.syncStatus == "SYNCED" && backupPreferences.observeIsBackupEnabled().first()) {
                syncQueueDao.insert(
                    SyncQueueEntity(
                        downloadId = record.id,
                        operation = "DELETE",
                        createdAt = System.currentTimeMillis(),
                    ),
                )
                syncManager.get().processPendingOperations()
            }
            downloadDao.delete(record.toEntity())
        }

        override suspend fun deleteAll() = downloadDao.deleteAll()
    }
