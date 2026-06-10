package com.socialvideodownloader.shared.data.cloud

/**
 * iOS [CloudHistoryDataSource] backed by the Swift [PlatformFirestoreProvider].
 *
 * A dumb adapter: it maps the seam's primitives onto the provider's string-document API and contains
 * no business logic (that lives in [CloudBackupRepositoryImpl]). The provider stores opaque JSON
 * strings, so each history document is encoded as
 *   `{"docId":..,"sourceUrlHash":..,"createdAt":N,"payload":"<hex>"}`
 * where `payload` is the hex-encoded encrypted bytes, and the counter doc as
 *   `{"recordCount":N,"tierLimit":M}` at `users/{uid}/meta/counters`.
 *
 * uid is supplied by the repository (resolved from CloudAuthService); the provider's own
 * `currentUid()` is intentionally ignored so there is a single source of identity.
 */
class PlatformFirestoreCloudHistoryDataSource(
    private val firestoreProvider: PlatformFirestoreProvider,
) : CloudHistoryDataSource {
    private fun historyPath(uid: String) = "users/$uid/history"

    private fun metaPath(uid: String) = "users/$uid/meta"

    override suspend fun recordExists(
        uid: String,
        docId: String,
    ): Boolean = firestoreProvider.getDocument(historyPath(uid), docId) != null

    override suspend fun putRecord(
        uid: String,
        document: CloudRecordDocument,
    ) {
        firestoreProvider.setDocument(historyPath(uid), document.docId, encodeDocument(document))
    }

    override suspend fun deleteRecordsByHash(
        uid: String,
        sourceUrlHash: String,
    ): Int {
        val matches = getAllRecords(uid).filter { it.sourceUrlHash == sourceUrlHash }
        matches.forEach { firestoreProvider.deleteDocument(historyPath(uid), it.docId) }
        return matches.size
    }

    override suspend fun getAllRecords(uid: String): List<CloudRecordDocument> =
        firestoreProvider.fetchCollection(historyPath(uid)).mapNotNull { decodeDocument(it) }

    override suspend fun getOldestRecords(
        uid: String,
        limit: Int,
    ): List<CloudRecordDocument> = getAllRecords(uid).sortedBy { it.createdAt }.take(limit)

    override suspend fun deleteRecordsByIds(
        uid: String,
        docIds: List<String>,
    ) {
        docIds.forEach { firestoreProvider.deleteDocument(historyPath(uid), it) }
    }

    override suspend fun getRecordCount(uid: String): Int = readMeta(uid).first

    override suspend fun adjustRecordCount(
        uid: String,
        delta: Int,
    ) {
        val (count, tierLimit) = readMeta(uid)
        writeMeta(uid, count + delta, tierLimit)
    }

    override suspend fun setRecordCount(
        uid: String,
        count: Int,
    ) {
        val (_, tierLimit) = readMeta(uid)
        writeMeta(uid, count, tierLimit)
    }

    override suspend fun getTierLimit(uid: String): Int? = readMeta(uid).second

    override suspend fun setTierLimit(
        uid: String,
        limit: Int,
    ) {
        val (count, _) = readMeta(uid)
        writeMeta(uid, count, limit)
    }

    // ---------------------------------------------------------------------------
    // Meta document (record counter + tier limit) — read/modify/write because the
    // provider's setDocument overwrites the whole document.
    // ---------------------------------------------------------------------------

    private suspend fun readMeta(uid: String): Pair<Int, Int?> {
        val json = firestoreProvider.getDocument(metaPath(uid), META_DOC_ID) ?: return 0 to null
        val count = extractLong(json, "recordCount")?.toInt() ?: 0
        val tierLimit = extractLong(json, "tierLimit")?.toInt()
        return count to tierLimit
    }

    private suspend fun writeMeta(
        uid: String,
        count: Int,
        tierLimit: Int?,
    ) {
        val json =
            buildString {
                append("{\"recordCount\":")
                append(count)
                if (tierLimit != null) {
                    append(",\"tierLimit\":")
                    append(tierLimit)
                }
                append("}")
            }
        firestoreProvider.setDocument(metaPath(uid), META_DOC_ID, json)
    }

    // ---------------------------------------------------------------------------
    // Document JSON (de)serialization. docId / sourceUrlHash / payload are all hex,
    // and createdAt is numeric, so no JSON string escaping is required.
    // ---------------------------------------------------------------------------

    private fun encodeDocument(document: CloudRecordDocument): String =
        buildString {
            append("{\"docId\":\"")
            append(document.docId)
            append("\",\"sourceUrlHash\":\"")
            append(document.sourceUrlHash)
            append("\",\"createdAt\":")
            append(document.createdAt)
            append(",\"payload\":\"")
            append(document.encryptedPayload.toHex())
            append("\"}")
        }

    private fun decodeDocument(json: String): CloudRecordDocument? {
        val docId = extractString(json, "docId") ?: return null
        val payload = extractString(json, "payload")?.hexToByteArrayOrNull() ?: return null
        return CloudRecordDocument(
            docId = docId,
            encryptedPayload = payload,
            createdAt = extractLong(json, "createdAt") ?: 0L,
            sourceUrlHash = extractString(json, "sourceUrlHash") ?: docId,
        )
    }

    private fun extractString(
        json: String,
        key: String,
    ): String? = Regex("\"$key\":\"([^\"]*)\"").find(json)?.groupValues?.get(1)

    private fun extractLong(
        json: String,
        key: String,
    ): Long? = Regex("\"$key\":(\\d+)").find(json)?.groupValues?.get(1)?.toLongOrNull()

    companion object {
        private const val META_DOC_ID = "counters"
    }
}

private fun ByteArray.toHex(): String = joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }

private fun String.hexToByteArrayOrNull(): ByteArray? {
    if (length % 2 != 0) return null
    return try {
        ByteArray(length / 2) { i ->
            ((this[i * 2].digitToInt(16) shl 4) or this[i * 2 + 1].digitToInt(16)).toByte()
        }
    } catch (_: IllegalArgumentException) {
        null
    }
}
