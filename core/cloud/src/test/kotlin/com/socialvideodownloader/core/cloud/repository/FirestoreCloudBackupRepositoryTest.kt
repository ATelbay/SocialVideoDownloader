package com.socialvideodownloader.core.cloud.repository

import com.socialvideodownloader.core.domain.model.DownloadRecord
import com.socialvideodownloader.core.domain.model.DownloadStatus
import com.socialvideodownloader.core.domain.sync.CloudAuthService
import com.socialvideodownloader.core.domain.sync.EncryptionService
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * Unit tests for [FirestoreCloudBackupRepository] against an in-memory [CloudHistoryDataSource].
 *
 * These lock in the counter-correctness behaviour that startup reconciliation can only paper over:
 * the counter must track the *actual* number of distinct documents, and re-uploads must never evict
 * data or inflate the count.
 */
class FirestoreCloudBackupRepositoryTest {
    private val uid = "user-1"
    private val authService = mockk<CloudAuthService>(relaxed = true)
    private lateinit var dataSource: FakeCloudHistoryDataSource
    private lateinit var repository: FirestoreCloudBackupRepository
    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setup() {
        every { authService.getCurrentUid() } returns uid
        dataSource = FakeCloudHistoryDataSource()
        repository =
            FirestoreCloudBackupRepository(
                authService = authService,
                encryptionService = FakeEncryptionService(),
                dataSource = dataSource,
                ioDispatcher = testDispatcher,
            )
    }

    private fun record(
        sourceUrl: String,
        createdAt: Long,
        title: String = "Title",
    ) = DownloadRecord(
        sourceUrl = sourceUrl,
        videoTitle = title,
        status = DownloadStatus.COMPLETED,
        createdAt = createdAt,
    )

    @Test
    fun `uploadRecord stores a new record and increments the counter by one`() =
        runTest(testDispatcher) {
            repository.uploadRecord(record("https://example.com/v1", 1000L))

            assertEquals(1, dataSource.getRecordCount(uid))
            assertEquals(1, dataSource.getAllRecords(uid).size)
        }

    @Test
    fun `re-uploading the same record overwrites without inflating the counter`() =
        runTest(testDispatcher) {
            val r = record("https://example.com/v1", 1000L)

            repository.uploadRecord(r)
            repository.uploadRecord(r) // retry / update of an already-synced record

            assertEquals(1, dataSource.getRecordCount(uid), "counter must not double-count a re-upload")
            assertEquals(1, dataSource.getAllRecords(uid).size)
        }

    @Test
    fun `uploadRecord evicts the oldest record when at the tier limit`() =
        runTest(testDispatcher) {
            repository.uploadRecord(record("https://example.com/v1", 1000L))
            repository.uploadRecord(record("https://example.com/v2", 2000L))
            repository.updateTierLimit(2)

            repository.uploadRecord(record("https://example.com/v3", 3000L))

            assertEquals(2, dataSource.getRecordCount(uid))
            val urls = repository.fetchAllRecords().map { it.sourceUrl }.toSet()
            assertEquals(setOf("https://example.com/v2", "https://example.com/v3"), urls)
        }

    @Test
    fun `re-uploading an existing record at the tier limit does not evict`() =
        runTest(testDispatcher) {
            val first = record("https://example.com/v1", 1000L)
            repository.uploadRecord(first)
            repository.uploadRecord(record("https://example.com/v2", 2000L))
            repository.updateTierLimit(2)

            repository.uploadRecord(first) // re-upload, not a new document

            assertEquals(2, dataSource.getRecordCount(uid))
            assertEquals(2, dataSource.getAllRecords(uid).size, "nothing should have been evicted")
        }

    @Test
    fun `deleteRecord removes the document and decrements the counter by the number removed`() =
        runTest(testDispatcher) {
            dataSource.seed(uid, CloudRecordDocument("h1", "payload".toByteArray(), 1000L, "h1"))
            dataSource.setRecordCount(uid, 1)

            val result = repository.deleteRecord("h1")

            assertTrue(result)
            assertEquals(0, dataSource.getRecordCount(uid))
            assertTrue(dataSource.getAllRecords(uid).isEmpty())
        }

    @Test
    fun `deleteRecord for a missing hash leaves the counter unchanged`() =
        runTest(testDispatcher) {
            dataSource.seed(uid, CloudRecordDocument("h1", "payload".toByteArray(), 1000L, "h1"))
            dataSource.setRecordCount(uid, 1)

            repository.deleteRecord("does-not-exist")

            assertEquals(1, dataSource.getRecordCount(uid), "deleting nothing must not drift the counter")
        }

    @Test
    fun `fetchAllRecords decrypts everything and skips undecryptable payloads`() =
        runTest(testDispatcher) {
            repository.uploadRecord(record("https://example.com/v1", 1000L))
            dataSource.seed(uid, CloudRecordDocument("garbage", "not-a-valid-payload".toByteArray(), 2000L, "garbage"))

            val restored = repository.fetchAllRecords()

            assertEquals(1, restored.size)
            assertEquals("https://example.com/v1", restored.single().sourceUrl)
        }

    @Test
    fun `getTierLimit returns the default when unset and the stored value when set`() =
        runTest(testDispatcher) {
            assertEquals(1000, repository.getTierLimit())

            repository.updateTierLimit(10_000)

            assertEquals(10_000, repository.getTierLimit())
        }

    @Test
    fun `uploadRecord throws when not authenticated`() =
        runTest(testDispatcher) {
            every { authService.getCurrentUid() } returns null

            assertThrows<IllegalStateException> {
                repository.uploadRecord(record("https://example.com/v1", 1000L))
            }
        }

    @Test
    fun `deleteRecord returns false and fetchAllRecords is empty when not authenticated`() =
        runTest(testDispatcher) {
            every { authService.getCurrentUid() } returns null

            assertFalse(repository.deleteRecord("h1"))
            assertTrue(repository.fetchAllRecords().isEmpty())
        }
}

/** Round-tripping fake: encodes a few record fields into bytes so encrypt/decrypt are reversible in tests. */
private class FakeEncryptionService : EncryptionService {
    override fun encrypt(record: DownloadRecord): ByteArray = "${record.sourceUrl}|${record.createdAt}|${record.videoTitle}".toByteArray()

    override fun decrypt(data: ByteArray): DownloadRecord {
        val parts = String(data).split("|")
        // Malformed payloads (e.g. seeded garbage) throw here, exercising the skip-on-failure path.
        return DownloadRecord(
            sourceUrl = parts[0],
            createdAt = parts[1].toLong(),
            videoTitle = parts[2],
            status = DownloadStatus.COMPLETED,
        )
    }

    override fun isKeyValid(): Boolean = true

    override fun regenerateKey() = Unit
}

/** In-memory [CloudHistoryDataSource]; the record counter is stored independently of the documents, like Firestore. */
private class FakeCloudHistoryDataSource : CloudHistoryDataSource {
    private val records = mutableMapOf<String, LinkedHashMap<String, CloudRecordDocument>>()
    private val counts = mutableMapOf<String, Int>()
    private val tierLimits = mutableMapOf<String, Int>()

    fun seed(
        uid: String,
        document: CloudRecordDocument,
    ) {
        store(uid)[document.docId] = document
    }

    private fun store(uid: String) = records.getOrPut(uid) { LinkedHashMap() }

    override suspend fun recordExists(
        uid: String,
        docId: String,
    ): Boolean = store(uid).containsKey(docId)

    override suspend fun putRecord(
        uid: String,
        document: CloudRecordDocument,
    ) {
        store(uid)[document.docId] = document
    }

    override suspend fun deleteRecordsByHash(
        uid: String,
        sourceUrlHash: String,
    ): Int {
        val s = store(uid)
        val ids = s.values.filter { it.sourceUrlHash == sourceUrlHash }.map { it.docId }
        ids.forEach { s.remove(it) }
        return ids.size
    }

    override suspend fun getAllRecords(uid: String): List<CloudRecordDocument> = store(uid).values.toList()

    override suspend fun getOldestRecords(
        uid: String,
        limit: Int,
    ): List<CloudRecordDocument> = store(uid).values.sortedBy { it.createdAt }.take(limit)

    override suspend fun deleteRecordsByIds(
        uid: String,
        docIds: List<String>,
    ) {
        val s = store(uid)
        docIds.forEach { s.remove(it) }
    }

    override suspend fun getRecordCount(uid: String): Int = counts[uid] ?: 0

    override suspend fun adjustRecordCount(
        uid: String,
        delta: Int,
    ) {
        counts[uid] = (counts[uid] ?: 0) + delta
    }

    override suspend fun setRecordCount(
        uid: String,
        count: Int,
    ) {
        counts[uid] = count
    }

    override suspend fun getTierLimit(uid: String): Int? = tierLimits[uid]

    override suspend fun setTierLimit(
        uid: String,
        limit: Int,
    ) {
        tierLimits[uid] = limit
    }
}
