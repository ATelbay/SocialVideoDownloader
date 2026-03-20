package com.socialvideodownloader.core.cloud.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.socialvideodownloader.core.domain.model.DownloadRecord
import com.socialvideodownloader.core.domain.repository.CloudBackupRepository
import com.socialvideodownloader.core.domain.sync.CloudAuthService
import com.socialvideodownloader.core.domain.sync.EncryptionService
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import javax.inject.Inject

class FirestoreCloudBackupRepository @Inject constructor(
    private val authService: CloudAuthService,
    private val encryptionService: EncryptionService,
) : CloudBackupRepository {

    private val firestore get() = Firebase.firestore

    private fun historyCollection(uid: String) =
        firestore.collection("users").document(uid).collection("history")

    private fun countersDocument(uid: String) =
        firestore.collection("users").document(uid).collection("meta").document("counters")

    override suspend fun uploadRecord(record: DownloadRecord): Boolean {
        val uid = authService.getCurrentUid() ?: authService.signInAnonymously()
        val encrypted = encryptionService.encrypt(record)
        val hash = sourceUrlHash(record.sourceUrl, record.createdAt)
        val data = mapOf(
            "encryptedPayload" to encrypted,
            "createdAt" to record.createdAt,
            "sourceUrlHash" to hash,
        )
        historyCollection(uid).document(hash).set(data).await()
        countersDocument(uid).update("recordCount", FieldValue.increment(1)).await()
        return true
    }

    override suspend fun deleteRecord(sourceUrlHash: String): Boolean {
        val uid = authService.getCurrentUid() ?: return false
        val query = historyCollection(uid)
            .whereEqualTo("sourceUrlHash", sourceUrlHash)
            .get()
            .await()
        for (doc in query.documents) {
            doc.reference.delete().await()
        }
        countersDocument(uid).update("recordCount", FieldValue.increment(-1)).await()
        return true
    }

    override suspend fun fetchAllRecords(): List<DownloadRecord> {
        val uid = authService.getCurrentUid() ?: return emptyList()
        val snapshot = historyCollection(uid).get().await()
        return snapshot.documents.mapNotNull { doc ->
            val payload = doc.getBlob("encryptedPayload")?.toBytes()
                ?: (doc.get("encryptedPayload") as? ByteArray)
                ?: return@mapNotNull null
            runCatching { encryptionService.decrypt(payload) }.getOrNull()
        }
    }

    override suspend fun getCloudRecordCount(): Int {
        val uid = authService.getCurrentUid() ?: return 0
        val doc = countersDocument(uid).get().await()
        return (doc.getLong("recordCount") ?: 0L).toInt()
    }

    override suspend fun getTierLimit(): Int {
        val uid = authService.getCurrentUid() ?: return 1000
        val doc = countersDocument(uid).get().await()
        return (doc.getLong("tierLimit") ?: 1000L).toInt()
    }

    override suspend fun updateTierLimit(limit: Int) {
        val uid = authService.getCurrentUid() ?: return
        countersDocument(uid).update("tierLimit", limit).await()
    }

    override suspend fun evictOldestRecords(count: Int) {
        val uid = authService.getCurrentUid() ?: return
        val oldest = historyCollection(uid)
            .orderBy("createdAt")
            .limit(count.toLong())
            .get()
            .await()
        val batch = firestore.batch()
        for (doc in oldest.documents) {
            batch.delete(doc.reference)
        }
        batch.commit().await()
        countersDocument(uid).update("recordCount", FieldValue.increment(-oldest.size().toLong())).await()
    }

    private fun sourceUrlHash(sourceUrl: String, createdAt: Long): String {
        val input = "$sourceUrl$createdAt"
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(input.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
}
