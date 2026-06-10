package com.socialvideodownloader.core.cloud.repository

/**
 * A single encrypted history document as stored in the cloud.
 *
 * [docId] is the deterministic SHA-256 hash of `sourceUrl + createdAt` and is also persisted as
 * the [sourceUrlHash] field, so a record always maps to the same document regardless of device.
 */
data class CloudRecordDocument(
    val docId: String,
    val encryptedPayload: ByteArray,
    val createdAt: Long,
    val sourceUrlHash: String,
)

/**
 * Thin persistence seam over the raw cloud store (Firestore).
 *
 * This interface exposes ONLY dumb I/O primitives — it holds no business logic. All the decision
 * logic that used to be entangled with Firestore (counter accounting, "increment only for new
 * documents", tier-limit enforcement, LRU eviction, encryption) lives in
 * [FirestoreCloudBackupRepository], which is now unit-testable against an in-memory fake of this
 * interface. The production implementation [FirestoreCloudHistoryDataSource] is a trivial adapter
 * around `Firebase.firestore` and is intentionally not unit-tested (exercised on-device instead).
 *
 * All methods take an explicit [uid] so the data source never needs auth; the repository resolves
 * the uid from [com.socialvideodownloader.core.domain.sync.CloudAuthService] and passes it in.
 */
interface CloudHistoryDataSource {
    /** True if a history document with [docId] already exists for [uid]. */
    suspend fun recordExists(
        uid: String,
        docId: String,
    ): Boolean

    /** Create or overwrite the history [document] for [uid]. Idempotent on [CloudRecordDocument.docId]. */
    suspend fun putRecord(
        uid: String,
        document: CloudRecordDocument,
    )

    /** Delete every history document whose `sourceUrlHash` equals [sourceUrlHash]. Returns how many were removed. */
    suspend fun deleteRecordsByHash(
        uid: String,
        sourceUrlHash: String,
    ): Int

    /** All history documents for [uid] (encrypted payloads, not decrypted). */
    suspend fun getAllRecords(uid: String): List<CloudRecordDocument>

    /** The [limit] oldest history documents for [uid], ordered by `createdAt` ascending. */
    suspend fun getOldestRecords(
        uid: String,
        limit: Int,
    ): List<CloudRecordDocument>

    /** Delete the history documents identified by [docIds] for [uid]. */
    suspend fun deleteRecordsByIds(
        uid: String,
        docIds: List<String>,
    )

    /** The stored record counter for [uid] (0 if absent). */
    suspend fun getRecordCount(uid: String): Int

    /** Atomically add [delta] (which may be negative) to the stored record counter for [uid]. */
    suspend fun adjustRecordCount(
        uid: String,
        delta: Int,
    )

    /** Overwrite the stored record counter for [uid] with [count]. Used for reconciliation. */
    suspend fun setRecordCount(
        uid: String,
        count: Int,
    )

    /** The stored tier limit for [uid], or null when unset (caller applies the default). */
    suspend fun getTierLimit(uid: String): Int?

    /** Persist [limit] as the tier limit for [uid]. */
    suspend fun setTierLimit(
        uid: String,
        limit: Int,
    )
}
