package com.socialvideodownloader.core.data.repository

import com.socialvideodownloader.core.data.local.DownloadDao
import com.socialvideodownloader.core.data.local.toDomain
import com.socialvideodownloader.core.data.local.toEntity
import com.socialvideodownloader.core.domain.model.DownloadRecord
import com.socialvideodownloader.core.domain.repository.DownloadRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DownloadRepositoryImpl @Inject constructor(
    private val downloadDao: DownloadDao,
) : DownloadRepository {

    override fun getAll(): Flow<List<DownloadRecord>> =
        downloadDao.getAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getById(id: Long): DownloadRecord? =
        downloadDao.getById(id)?.toDomain()

    override suspend fun insert(record: DownloadRecord): Long =
        downloadDao.insert(record.toEntity())

    override suspend fun updateStatus(record: DownloadRecord) =
        downloadDao.update(record.toEntity())

    override suspend fun delete(record: DownloadRecord) =
        downloadDao.delete(record.toEntity())
}
