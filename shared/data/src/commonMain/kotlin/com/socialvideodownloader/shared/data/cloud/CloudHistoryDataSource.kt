package com.socialvideodownloader.shared.data.cloud

/**
 * A single encrypted history document as stored in the cloud.
 *
 * [docId] is the deterministic id derived from `sourceUrl + createdAt` (see [cloudDocumentId]) and is
 * also persisted as the [sourceUrlHash] field, so a record always maps to the same document.
 */
data class CloudRecordDocument(
    val docId: String,
    val encryptedPayload: ByteArray,
    val createdAt: Long,
    val sourceUrlHash: String,
)

/**
 * Thin persistence seam over the raw cloud store.
 *
 * This is the KMP twin of the Android `:core:cloud` `CloudHistoryDataSource`: it exposes ONLY dumb
 * I/O primitives, so all the decision logic (counter accounting, "increment only for new documents",
 * tier-limit enforcement, LRU eviction, encryption) lives in [CloudBackupRepositoryImpl] and is
 * unit-testable on the JVM against an in-memory fake. The production iOS implementation
 * [PlatformFirestoreCloudHistoryDataSource] is a trivial adapter over the Swift
 * [PlatformFirestoreProvider] and is intentionally not unit-tested.
 *
 * All methods take an explicit [uid] so the data source never needs auth; the repository resolves the
 * uid from [com.socialvideodownloader.core.domain.sync.CloudAuthService] and passes it in.
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

    /** Add [delta] (which may be negative) to the stored record counter for [uid]. */
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

/**
 * Deterministic cloud document id for a record, derived from `sourceUrl + createdAt`.
 *
 * Delegates to the canonical [com.socialvideodownloader.core.domain.sync.cloudDocumentId]
 * (lowercase-hex SHA-256), so Android and iOS produce byte-identical ids for the same record —
 * the same account maps to the same documents across devices. Kept as a thin alias so existing
 * call sites (upload, delete, restore, sync) stay unchanged.
 *
 * Note: this changes the iOS id scheme (previously FNV-1a). Restore reads the whole collection
 * (id-agnostic), so no data is lost; the record counter self-corrects via reconciliation.
 */
internal fun cloudDocumentId(
    sourceUrl: String,
    createdAt: Long,
): String = com.socialvideodownloader.core.domain.sync.cloudDocumentId(sourceUrl, createdAt)
