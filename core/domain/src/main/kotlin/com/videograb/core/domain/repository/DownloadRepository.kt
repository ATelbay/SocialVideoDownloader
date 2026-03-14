package com.videograb.core.domain.repository

import com.videograb.core.domain.model.DownloadRecord
import kotlinx.coroutines.flow.Flow

interface DownloadRepository {
    fun getAll(): Flow<List<DownloadRecord>>

    suspend fun getById(id: Long): DownloadRecord?

    suspend fun insert(record: DownloadRecord): Long

    suspend fun updateStatus(record: DownloadRecord)

    suspend fun delete(record: DownloadRecord)
}
