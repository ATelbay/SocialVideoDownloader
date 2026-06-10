package com.socialvideodownloader.shared.data.cloud

import com.socialvideodownloader.core.domain.model.DownloadRecord
import com.socialvideodownloader.core.domain.model.DownloadStatus
import com.socialvideodownloader.core.domain.sync.CloudAuthService
import com.socialvideodownloader.core.domain.sync.EncryptionService
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for the KMP [CloudBackupRepositoryImpl] against an in-memory [CloudHistoryDataSource].
 *
 * Mirrors the Android `FirestoreCloudBackupRepositoryTest`: locks in counter correctness (track the
 * actual number of distinct documents; re-uploads never evict or inflate) for the iOS code path.
 * Runs on the JVM via the Android test target.
 */
class CloudBackupRepositoryImplTest {
    private val uid = "user-1"
    private lateinit var authService: FakeCloudAuthService
    private lateinit var dataSource: FakeCloudHistoryDataSource
    private lateinit var repository: CloudBackupRepositoryImpl
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        authService = FakeCloudAuthService(uid)
        dataSource = FakeCloudHistoryDataSource()
        repository =
            CloudBackupRepositoryImpl(
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
    fun uploadRecordStoresNewRecordAndIncrementsCounterByOne() =
        runTest(testDispatcher) {
            repository.uploadRecord(record("https://example.com/v1", 1000L))

            assertEquals(1, dataSource.getRecordCount(uid))
            assertEquals(1, dataSource.getAllRecords(uid).size)
        }

    @Test
    fun reUploadingSameRecordOverwritesWithoutInflatingCounter() =
        runTest(testDispatcher) {
            val r = record("https://example.com/v1", 1000L)

            repository.uploadRecord(r)
            repository.uploadRecord(r)

            assertEquals(1, dataSource.getRecordCount(uid))
            assertEquals(1, dataSource.getAllRecords(uid).size)
        }

    @Test
    fun uploadRecordEvictsOldestWhenAtTierLimit() =
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
    fun reUploadingExistingRecordAtTierLimitDoesNotEvict() =
        runTest(testDispatcher) {
            val first = record("https://example.com/v1", 1000L)
            repository.uploadRecord(first)
            repository.uploadRecord(record("https://example.com/v2", 2000L))
            repository.updateTierLimit(2)

            repository.uploadRecord(first)

            assertEquals(2, dataSource.getRecordCount(uid))
            assertEquals(2, dataSource.getAllRecords(uid).size)
        }

    @Test
    fun deleteRecordRemovesDocumentAndDecrementsCounterByNumberRemoved() =
        runTest(testDispatcher) {
            repository.uploadRecord(record("https://example.com/v1", 1000L))
            val docId = cloudDocumentId("https://example.com/v1", 1000L)

            val result = repository.deleteRecord(docId)

            assertTrue(result)
            assertEquals(0, dataSource.getRecordCount(uid))
            assertTrue(dataSource.getAllRecords(uid).isEmpty())
        }

    @Test
    fun deleteRecordForMissingHashLeavesCounterUnchanged() =
        runTest(testDispatcher) {
            repository.uploadRecord(record("https://example.com/v1", 1000L))

            repository.deleteRecord("does-not-exist")

            assertEquals(1, dataSource.getRecordCount(uid))
        }

    @Test
    fun fetchAllRecordsDecryptsEverythingAndSkipsUndecryptablePayloads() =
        runTest(testDispatcher) {
            repository.uploadRecord(record("https://example.com/v1", 1000L))
            dataSource.seed(uid, CloudRecordDocument("garbage", "not-valid".encodeToByteArray(), 2000L, "garbage"))

            val restored = repository.fetchAllRecords()

            assertEquals(1, restored.size)
            assertEquals("https://example.com/v1", restored.single().sourceUrl)
        }

    @Test
    fun getTierLimitReturnsDefaultWhenUnsetAndStoredValueWhenSet() =
        runTest(testDispatcher) {
            assertEquals(1000, repository.getTierLimit())

            repository.updateTierLimit(10_000)

            assertEquals(10_000, repository.getTierLimit())
        }

    @Test
    fun uploadRecordThrowsWhenNotAuthenticated() =
        runTest(testDispatcher) {
            authService.uid = null

            assertFailsWith<IllegalStateException> {
                repository.uploadRecord(record("https://example.com/v1", 1000L))
            }
        }

    @Test
    fun deleteRecordReturnsFalseAndFetchAllRecordsIsEmptyWhenNotAuthenticated() =
        runTest(testDispatcher) {
            authService.uid = null

            assertFalse(repository.deleteRecord("any"))
            assertTrue(repository.fetchAllRecords().isEmpty())
        }
}

private class FakeCloudAuthService(var uid: String?) : CloudAuthService {
    override suspend fun signInWithGoogleCredential(idToken: String): String = uid ?: error("not signed in")

    override fun getCurrentUid(): String? = uid

    override fun isAuthenticated(): Boolean = uid != null

    override suspend fun signOut() {
        uid = null
    }

    override fun getDisplayName(): String? = null

    override fun getPhotoUrl(): String? = null
}

/** Round-tripping fake: encodes a few record fields into bytes so encrypt/decrypt are reversible in tests. */
private class FakeEncryptionService : EncryptionService {
    override fun encrypt(record: DownloadRecord): ByteArray =
        "${record.sourceUrl}|${record.createdAt}|${record.videoTitle}".encodeToByteArray()

    override fun decrypt(data: ByteArray): DownloadRecord {
        val parts = data.decodeToString().split("|")
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

/** In-memory [CloudHistoryDataSource]; the record counter is stored independently of the documents. */
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
