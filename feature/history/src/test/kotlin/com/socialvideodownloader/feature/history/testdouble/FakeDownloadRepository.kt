package com.socialvideodownloader.feature.history.testdouble

import com.socialvideodownloader.core.domain.model.DownloadRecord
import com.socialvideodownloader.core.domain.repository.DownloadRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class FakeDownloadRepository : DownloadRepository {

    val recordsFlow = MutableSharedFlow<List<DownloadRecord>>(replay = 1)

    val deletedRecords = mutableListOf<DownloadRecord>()
    var deleteAllCalled = false
    var onDeleteCallback: (() -> Unit)? = null

    override fun getAll(): Flow<List<DownloadRecord>> = recordsFlow

    override suspend fun getById(id: Long): DownloadRecord? {
        return recordsFlow.replayCache.firstOrNull()?.find { it.id == id }
    }

    override suspend fun insert(record: DownloadRecord): Long {
        val current = recordsFlow.replayCache.firstOrNull() ?: emptyList()
        val newRecord = record.copy(id = record.id.takeIf { it != 0L } ?: (current.size + 1L))
        recordsFlow.emit(current + newRecord)
        return newRecord.id
    }

    override suspend fun update(record: DownloadRecord) {
        val current = recordsFlow.replayCache.firstOrNull() ?: emptyList()
        recordsFlow.emit(current.map { if (it.id == record.id) record else it })
    }

    override suspend fun delete(record: DownloadRecord) {
        onDeleteCallback?.invoke()
        deletedRecords.add(record)
        val current = recordsFlow.replayCache.firstOrNull() ?: emptyList()
        recordsFlow.emit(current.filter { it.id != record.id })
    }

    override suspend fun deleteAll() {
        deleteAllCalled = true
        recordsFlow.emit(emptyList())
    }
}
