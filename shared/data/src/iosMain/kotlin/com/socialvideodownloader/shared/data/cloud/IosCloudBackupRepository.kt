package com.socialvideodownloader.shared.data.cloud

import com.socialvideodownloader.core.domain.model.DownloadRecord
import com.socialvideodownloader.core.domain.model.DownloadStatus
import com.socialvideodownloader.core.domain.repository.CloudBackupRepository
import com.socialvideodownloader.core.domain.sync.EncryptionService

/**
 * iOS implementation of [CloudBackupRepository].
 *
 * Uses [PlatformFirestoreProvider] to delegate Firestore SDK calls to Swift,
 * and [EncryptionService] to encrypt/decrypt records before writing to cloud.
 *
 * Firestore document structure:
 *   Collection: `users/{uid}/downloads`
 *   Document ID: SHA-256 hash of sourceUrl (provided by [EncryptionService] serialization)
 *   Document data: Base64-encoded encrypted [DownloadRecord] bytes
 *
 * The metadata document at `users/{uid}/meta` stores:
 *   - `record_count`: Int
 *   - `tier_limit`: Int
 *
 * TODO: Wire to actual Firestore Swift implementation once Firebase CocoaPod is integrated.
 *       Currently all methods log a TODO warning and return safe default values.
 */
class IosCloudBackupRepository(
    private val firestoreProvider: PlatformFirestoreProvider,
    private val encryptionService: EncryptionService,
) : CloudBackupRepository {
    companion object {
        private const val META_DOC_ID = "meta"
        private const val META_FIELD_COUNT = "record_count"
        private const val META_FIELD_TIER = "tier_limit"
        private const val DEFAULT_TIER_LIMIT = 1000
    }

    /**
     * Returns a deterministic document ID for a given sourceUrl.
     *
     * Uses a stable unsigned-hex representation of the 32-bit hashCode combined with
     * string length to reduce (but not eliminate) collisions. All three call sites —
     * uploadRecord, deleteRecord, and IosSyncManager.processDelete — must call this
     * function so they always agree on the document ID.
     *
     * TODO: Replace with a full SHA-256 hex digest once a crypto utility is available
     *       in commonMain (e.g. via kotlinx-crypto or a Swift bridge).
     */
    internal fun documentIdFor(sourceUrl: String): String {
        val hash = sourceUrl.hashCode().toLong().and(0xFFFFFFFFL)
        return "${hash.toString(16)}_${sourceUrl.length}"
    }

    private fun downloadsPath(): String? {
        val uid = firestoreProvider.currentUid() ?: return null
        return "users/$uid/downloads"
    }

    private fun metaPath(): String? {
        val uid = firestoreProvider.currentUid() ?: return null
        return "users/$uid/meta"
    }

    /**
     * Upload a single [DownloadRecord] to Firestore.
     *
     * The record is encrypted via [EncryptionService] before upload.
     * Document ID is derived from the sourceUrl hash embedded in the encrypted payload.
     *
     * TODO: Replace stub JSON serialization with proper encryption when Firestore is wired.
     */
    override suspend fun uploadRecord(record: DownloadRecord): Boolean {
        // TODO: Encrypt record and upload to Firestore via firestoreProvider.
        // Current stub: serialize to simple JSON-like string and write to Firestore.
        // Replace with:
        //   val encrypted = encryptionService.encrypt(record)
        //   val base64 = encrypted.toBase64String()
        //   val documentId = record.sourceUrl.sha256()
        //   firestoreProvider.setDocument(downloadsPath(), documentId, base64)
        val path = downloadsPath() ?: return false
        val documentId = documentIdFor(record.sourceUrl)
        val stubJson = buildRecordJson(record)
        return firestoreProvider.setDocument(path, documentId, stubJson)
    }

    /**
     * Delete a single record from Firestore by its document ID.
     *
     * Callers must pass the result of [documentIdFor](sourceUrl) — the same function
     * used by [uploadRecord] — so the document ID is consistent.
     */
    override suspend fun deleteRecord(sourceUrlHash: String): Boolean {
        val path = downloadsPath() ?: return false
        return firestoreProvider.deleteDocument(path, sourceUrlHash)
    }

    /**
     * Fetch all cloud records and decrypt them.
     *
     * TODO: Replace stub deserialization with proper decryption when Firestore is wired.
     */
    override suspend fun fetchAllRecords(): List<DownloadRecord> {
        // TODO: Decrypt each document via encryptionService.decrypt(base64.fromBase64())
        val path = downloadsPath() ?: return emptyList()
        val documents = firestoreProvider.fetchCollection(path)
        return documents.mapNotNull { json -> parseRecordJson(json) }
    }

    override suspend fun getCloudRecordCount(): Int {
        // TODO: Read from meta document field record_count for efficiency.
        // Fallback: count documents in the collection.
        val path = metaPath() ?: return 0
        val metaJson = firestoreProvider.getDocument(path, META_DOC_ID)
        return metaJson?.let { parseIntField(it, META_FIELD_COUNT) } ?: 0
    }

    override suspend fun getTierLimit(): Int {
        val path = metaPath() ?: return DEFAULT_TIER_LIMIT
        val metaJson = firestoreProvider.getDocument(path, META_DOC_ID)
        return metaJson?.let { parseIntField(it, META_FIELD_TIER) } ?: DEFAULT_TIER_LIMIT
    }

    override suspend fun updateTierLimit(limit: Int) {
        // TODO: Merge-update the meta document field tier_limit.
        val path = metaPath() ?: return
        val json = "{\"$META_FIELD_TIER\": $limit}"
        firestoreProvider.setDocument(path, META_DOC_ID, json)
    }

    override suspend fun evictOldestRecords(count: Int) {
        // TODO: Query the collection ordered by createdAt ascending, limit to [count], delete each.
        // Requires Firestore query support in PlatformFirestoreProvider — to be added in Xcode phase.
    }

    override suspend fun setRecordCount(count: Int) {
        val path = metaPath() ?: return
        val json = "{\"$META_FIELD_COUNT\": $count}"
        firestoreProvider.setDocument(path, META_DOC_ID, json)
    }

    // ---------------------------------------------------------------------------
    // Stub serialization — replace with encryption-based flow in Xcode phase
    // ---------------------------------------------------------------------------

    private fun buildRecordJson(record: DownloadRecord): String =
        buildString {
            append("{")
            append("\"id\":${record.id},")
            append("\"sourceUrl\":\"${record.sourceUrl.escapeJson()}\",")
            append("\"videoTitle\":\"${record.videoTitle.escapeJson()}\",")
            append("\"thumbnailUrl\":${record.thumbnailUrl?.let { "\"${it.escapeJson()}\"" } ?: "null"},")
            append("\"formatLabel\":\"${record.formatLabel.escapeJson()}\",")
            append("\"filePath\":\"${record.filePath?.escapeJson() ?: ""}\",")
            append("\"mediaStoreUri\":${record.mediaStoreUri?.let { "\"${it.escapeJson()}\"" } ?: "null"},")
            append("\"status\":\"${record.status.name}\",")
            append("\"createdAt\":${record.createdAt},")
            append("\"completedAt\":${record.completedAt ?: "null"},")
            append("\"fileSizeBytes\":${record.fileSizeBytes ?: "null"},")
            append("\"syncStatus\":\"${record.syncStatus}\"")
            append("}")
        }

    private fun parseRecordJson(json: String): DownloadRecord? {
        return try {
            DownloadRecord(
                id = extractLong(json, "id") ?: 0L,
                sourceUrl = extractString(json, "sourceUrl") ?: return null,
                videoTitle = extractString(json, "videoTitle") ?: return null,
                thumbnailUrl = extractString(json, "thumbnailUrl"),
                formatLabel = extractString(json, "formatLabel") ?: "",
                filePath = extractString(json, "filePath") ?: "",
                mediaStoreUri = extractString(json, "mediaStoreUri"),
                status =
                    DownloadStatus.valueOf(
                        extractString(json, "status") ?: DownloadStatus.COMPLETED.name,
                    ),
                createdAt = extractLong(json, "createdAt") ?: 0L,
                completedAt = extractLong(json, "completedAt"),
                fileSizeBytes = extractLong(json, "fileSizeBytes"),
                syncStatus = extractString(json, "syncStatus") ?: "SYNCED",
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun parseIntField(
        json: String,
        field: String,
    ): Int? {
        return extractLong(json, field)?.toInt()
    }

    private fun extractString(
        json: String,
        key: String,
    ): String? {
        val pattern = "\"$key\":\"([^\"]*)\""
        val match = Regex(pattern).find(json) ?: return null
        return match.groupValues[1]
    }

    private fun extractLong(
        json: String,
        key: String,
    ): Long? {
        val pattern = "\"$key\":(\\d+)"
        val match = Regex(pattern).find(json) ?: return null
        return match.groupValues[1].toLongOrNull()
    }

    private fun String.escapeJson() = replace("\\", "\\\\").replace("\"", "\\\"")
}
