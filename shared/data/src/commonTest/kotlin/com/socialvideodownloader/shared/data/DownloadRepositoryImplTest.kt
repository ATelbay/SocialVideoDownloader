package com.socialvideodownloader.shared.data

import com.socialvideodownloader.core.domain.model.DownloadRecord
import com.socialvideodownloader.core.domain.model.DownloadStatus
import com.socialvideodownloader.shared.data.local.DownloadDao
import com.socialvideodownloader.shared.data.local.DownloadEntity
import com.socialvideodownloader.shared.data.repository.DownloadRepositoryImpl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Unit tests for [DownloadRepositoryImpl] using an in-memory fake DAO.
 *
 * Note: A full integration test with Room's in-memory database requires
 * Android instrumentation or a JVM test runner with Room testing support.
 * These tests verify the repository's mapping and delegation logic using
 * a manual fake implementation.
 */
class DownloadRepositoryImplTest {

    private val fakeDao = FakeDownloadDao()
    private val repository = DownloadRepositoryImpl(fakeDao)

    @Test
    fun insertAndGetById() = runTest {
        val record = createRecord(sourceUrl = "https://youtube.com/watch?v=1")
        val id = repository.insert(record)

        val retrieved = repository.getById(id)
        assertNotNull(retrieved)
        assertEquals("https://youtube.com/watch?v=1", retrieved.sourceUrl)
    }

    @Test
    fun getAllReturnsInsertedRecords() = runTest {
        repository.insert(createRecord(sourceUrl = "https://url1.com"))
        repository.insert(createRecord(sourceUrl = "https://url2.com"))

        val all = repository.getAll().first()
        assertEquals(2, all.size)
    }

    @Test
    fun updateModifiesRecord() = runTest {
        val id = repository.insert(createRecord(sourceUrl = "https://example.com"))
        val record = repository.getById(id)!!

        val updated = record.copy(videoTitle = "Updated Title")
        repository.update(updated)

        val retrieved = repository.getById(id)
        assertEquals("Updated Title", retrieved?.videoTitle)
    }

    @Test
    fun deleteRemovesRecord() = runTest {
        val id = repository.insert(createRecord(sourceUrl = "https://delete.me"))
        val record = repository.getById(id)!!

        repository.delete(record)

        assertNull(repository.getById(id))
    }

    @Test
    fun deleteAllClearsAllRecords() = runTest {
        repository.insert(createRecord(sourceUrl = "https://1.com"))
        repository.insert(createRecord(sourceUrl = "https://2.com"))

        repository.deleteAll()

        val all = repository.getAll().first()
        assertEquals(0, all.size)
    }

    @Test
    fun getCompletedDownloadsFiltersCorrectly() = runTest {
        repository.insert(createRecord(status = DownloadStatus.COMPLETED))
        repository.insert(createRecord(status = DownloadStatus.FAILED))
        repository.insert(createRecord(status = DownloadStatus.COMPLETED))

        val completed = repository.getCompletedDownloads().first()
        assertEquals(2, completed.size)
    }

    private var clock = 1000L
    private fun now() = clock++

    private fun createRecord(
        sourceUrl: String = "https://example.com",
        status: DownloadStatus = DownloadStatus.COMPLETED,
    ) = DownloadRecord(
        sourceUrl = sourceUrl,
        videoTitle = "Test Video",
        status = status,
        createdAt = now(),
        completedAt = if (status == DownloadStatus.COMPLETED) now() else null,
    )

    /** Fake in-memory DAO for testing without Room. */
    private class FakeDownloadDao : DownloadDao {
        private var autoId = 1L
        private val entities = mutableListOf<DownloadEntity>()
        private val flow = MutableStateFlow<List<DownloadEntity>>(emptyList())

        override fun getAll(): Flow<List<DownloadEntity>> = flow

        override suspend fun getById(id: Long): DownloadEntity? =
            entities.find { it.id == id }

        override suspend fun insert(entity: DownloadEntity): Long {
            val id = autoId++
            entities.add(entity.copy(id = id))
            flow.value = entities.toList()
            return id
        }

        override suspend fun update(entity: DownloadEntity) {
            val index = entities.indexOfFirst { it.id == entity.id }
            if (index >= 0) {
                entities[index] = entity
                flow.value = entities.toList()
            }
        }

        override suspend fun delete(entity: DownloadEntity) {
            entities.removeAll { it.id == entity.id }
            flow.value = entities.toList()
        }

        override suspend fun deleteAll() {
            entities.clear()
            flow.value = emptyList()
        }

        override fun getCompleted(): Flow<List<DownloadEntity>> =
            flow.map { list -> list.filter { it.status == "COMPLETED" } }

        override suspend fun getCompletedSnapshot(): List<DownloadEntity> =
            entities.filter { it.status == "COMPLETED" }
    }

}
