package com.videograb.core.data.repository

import com.videograb.core.data.local.DownloadDao
import com.videograb.core.data.local.toDomain
import com.videograb.core.data.local.toEntity
import com.videograb.core.domain.model.DownloadRecord
import com.videograb.core.domain.repository.DownloadRepository
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
