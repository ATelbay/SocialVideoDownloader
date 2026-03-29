package com.socialvideodownloader.shared.data.repository

import com.socialvideodownloader.core.domain.model.DownloadRecord
import com.socialvideodownloader.core.domain.model.DownloadStatus
import com.socialvideodownloader.core.domain.repository.DownloadRepository
import com.socialvideodownloader.shared.data.local.DownloadDao
import com.socialvideodownloader.shared.data.local.SyncQueueDao
import com.socialvideodownloader.shared.data.local.SyncQueueEntity
import com.socialvideodownloader.shared.data.local.toDomain
import com.socialvideodownloader.shared.data.local.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Shared KMP implementation of [DownloadRepository].
 *
 * This is a pure data access layer using the shared Room KMP DAOs.
 * Cloud sync logic (SyncManager integration) is handled by the platform-specific
 * wrapper in core/data on Android, or directly in the iOS Koin module.
 *
 * This shared implementation does NOT depend on BackupPreferences or SyncManager
 * to keep it free of platform-specific cloud dependencies. The Android
 * DownloadRepositoryImpl in core/data wraps this to add sync queue logic.
 */
class DownloadRepositoryImpl(
    private val downloadDao: DownloadDao,
) : DownloadRepository {

    override fun getAll(): Flow<List<DownloadRecord>> =
        downloadDao.getAll().map { entities -> entities.map { it.toDomain() } }

    override fun getCompletedDownloads(): Flow<List<DownloadRecord>> =
        downloadDao.getCompleted().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getById(id: Long): DownloadRecord? =
        downloadDao.getById(id)?.toDomain()

    override suspend fun getCompletedSnapshot(): List<DownloadRecord> =
        downloadDao.getCompletedSnapshot().map { it.toDomain() }

    override suspend fun insert(record: DownloadRecord): Long =
        downloadDao.insert(record.toEntity())

    override suspend fun update(record: DownloadRecord) =
        downloadDao.update(record.toEntity())

    override suspend fun delete(record: DownloadRecord) =
        downloadDao.delete(record.toEntity())

    override suspend fun deleteAll() =
        downloadDao.deleteAll()
}
