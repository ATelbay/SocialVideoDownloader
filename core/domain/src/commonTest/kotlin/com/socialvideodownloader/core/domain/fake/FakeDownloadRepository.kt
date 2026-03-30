package com.socialvideodownloader.core.domain.fake

import com.socialvideodownloader.core.domain.model.DownloadRecord
import com.socialvideodownloader.core.domain.repository.DownloadRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeDownloadRepository : DownloadRepository {
    private val records = MutableStateFlow<List<DownloadRecord>>(emptyList())
    private var nextId = 1L
    var insertException: Exception? = null
    var insertedRecords = mutableListOf<DownloadRecord>()

    fun setRecords(list: List<DownloadRecord>) {
        records.value = list
    }

    override fun getAll(): Flow<List<DownloadRecord>> = records

    override fun getCompletedDownloads(): Flow<List<DownloadRecord>> =
        records.map { list ->
            list.filter { it.status == com.socialvideodownloader.core.domain.model.DownloadStatus.COMPLETED }
        }

    override suspend fun getById(id: Long): DownloadRecord? = records.value.firstOrNull { it.id == id }

    override suspend fun getCompletedSnapshot(): List<DownloadRecord> =
        records.value.filter { it.status == com.socialvideodownloader.core.domain.model.DownloadStatus.COMPLETED }

    override suspend fun insert(record: DownloadRecord): Long {
        insertException?.let { throw it }
        val id = nextId++
        insertedRecords.add(record)
        records.value = records.value + record.copy(id = id)
        return id
    }

    override suspend fun update(record: DownloadRecord) {
        records.value = records.value.map { if (it.id == record.id) record else it }
    }

    override suspend fun delete(record: DownloadRecord) {
        records.value = records.value.filter { it.id != record.id }
    }

    override suspend fun deleteAll() {
        records.value = emptyList()
    }
}
