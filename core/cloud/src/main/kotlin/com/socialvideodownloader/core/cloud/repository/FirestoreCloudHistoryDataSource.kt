package com.socialvideodownloader.core.cloud.repository

import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Production [CloudHistoryDataSource] backed by Cloud Firestore.
 *
 * Intentionally a dumb adapter: it maps the seam's primitives onto raw Firestore calls and contains
 * no business logic. Document layout:
 *   - `users/{uid}/history/{docId}` — one encrypted record (fields: encryptedPayload, createdAt, sourceUrlHash)
 *   - `users/{uid}/meta/counters`  — { recordCount, tierLimit }
 */
class FirestoreCloudHistoryDataSource
    @Inject
    constructor() : CloudHistoryDataSource {
        private val firestore get() = Firebase.firestore

        private fun historyCollection(uid: String) = firestore.collection("users").document(uid).collection("history")

        private fun countersDocument(uid: String) = firestore.collection("users").document(uid).collection("meta").document("counters")

        override suspend fun recordExists(
            uid: String,
            docId: String,
        ): Boolean = historyCollection(uid).document(docId).get().await().exists()

        override suspend fun putRecord(
            uid: String,
            document: CloudRecordDocument,
        ) {
            val data =
                mapOf(
                    "encryptedPayload" to Blob.fromBytes(document.encryptedPayload),
                    "createdAt" to document.createdAt,
                    "sourceUrlHash" to document.sourceUrlHash,
                )
            historyCollection(uid).document(document.docId).set(data).await()
        }

        override suspend fun deleteRecordsByHash(
            uid: String,
            sourceUrlHash: String,
        ): Int {
            val query =
                historyCollection(uid)
                    .whereEqualTo("sourceUrlHash", sourceUrlHash)
                    .get()
                    .await()
            for (doc in query.documents) {
                doc.reference.delete().await()
            }
            return query.size()
        }

        override suspend fun getAllRecords(uid: String): List<CloudRecordDocument> {
            val snapshot = historyCollection(uid).get().await()
            return snapshot.documents.mapNotNull { doc ->
                val payload = doc.getBlob("encryptedPayload")?.toBytes() ?: return@mapNotNull null
                CloudRecordDocument(
                    docId = doc.id,
                    encryptedPayload = payload,
                    createdAt = doc.getLong("createdAt") ?: 0L,
                    sourceUrlHash = doc.getString("sourceUrlHash") ?: doc.id,
                )
            }
        }

        override suspend fun getOldestRecords(
            uid: String,
            limit: Int,
        ): List<CloudRecordDocument> {
            val snapshot =
                historyCollection(uid)
                    .orderBy("createdAt")
                    .limit(limit.toLong())
                    .get()
                    .await()
            return snapshot.documents.map { doc ->
                CloudRecordDocument(
                    docId = doc.id,
                    encryptedPayload = doc.getBlob("encryptedPayload")?.toBytes() ?: ByteArray(0),
                    createdAt = doc.getLong("createdAt") ?: 0L,
                    sourceUrlHash = doc.getString("sourceUrlHash") ?: doc.id,
                )
            }
        }

        override suspend fun deleteRecordsByIds(
            uid: String,
            docIds: List<String>,
        ) {
            if (docIds.isEmpty()) return
            val batch = firestore.batch()
            for (id in docIds) {
                batch.delete(historyCollection(uid).document(id))
            }
            batch.commit().await()
        }

        override suspend fun getRecordCount(uid: String): Int {
            val doc = countersDocument(uid).get().await()
            return (doc.getLong("recordCount") ?: 0L).toInt()
        }

        override suspend fun adjustRecordCount(
            uid: String,
            delta: Int,
        ) {
            countersDocument(uid)
                .set(mapOf("recordCount" to FieldValue.increment(delta.toLong())), SetOptions.merge())
                .await()
        }

        override suspend fun setRecordCount(
            uid: String,
            count: Int,
        ) {
            countersDocument(uid).set(mapOf("recordCount" to count.toLong()), SetOptions.merge()).await()
        }

        override suspend fun getTierLimit(uid: String): Int? {
            val doc = countersDocument(uid).get().await()
            return doc.getLong("tierLimit")?.toInt()
        }

        override suspend fun setTierLimit(
            uid: String,
            limit: Int,
        ) {
            countersDocument(uid).set(mapOf("tierLimit" to limit), SetOptions.merge()).await()
        }
    }
