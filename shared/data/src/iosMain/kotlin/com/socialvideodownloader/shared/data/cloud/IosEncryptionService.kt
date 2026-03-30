package com.socialvideodownloader.shared.data.cloud

import com.socialvideodownloader.core.domain.model.DownloadRecord
import com.socialvideodownloader.core.domain.model.DownloadStatus
import com.socialvideodownloader.core.domain.sync.EncryptionService

/**
 * iOS implementation of [EncryptionService].
 *
 * On the full implementation path this would use:
 *   - iOS Keychain for secure key storage (via Security.framework)
 *   - CryptoKit AES-GCM for symmetric encryption/decryption
 *
 * Both Keychain and CryptoKit are Swift-native APIs and are best called from Swift.
 * The [PlatformEncryptionProvider] bridge interface delegates those calls to Swift.
 *
 * Current state: stub implementation that performs no encryption (identity transform).
 * TODO: Replace with real encryption once Firebase/CryptoKit wiring is done in Xcode.
 *
 * The data format (once fully implemented):
 *   - encrypt: serialize record to JSON bytes, then AES-GCM encrypt with Keychain-stored key
 *   - decrypt: AES-GCM decrypt, then deserialize JSON bytes to DownloadRecord
 */
class IosEncryptionService : EncryptionService {
    /**
     * Stub: converts the record to a UTF-8 JSON byte array without encryption.
     *
     * TODO: Replace with AES-GCM encryption via CryptoKit:
     * ```swift
     * let key = try loadOrCreateKey(inKeychain: "com.svd.backup.key")
     * let sealedBox = try AES.GCM.seal(jsonBytes, using: key)
     * return sealedBox.combined!
     * ```
     */
    override fun encrypt(record: DownloadRecord): ByteArray {
        // TODO: Encrypt via CryptoKit AES-GCM using Keychain-stored key.
        // For now, encode as UTF-8 JSON — safe for development but not for production.
        val json = serializeToJson(record)
        return json.encodeToByteArray()
    }

    /**
     * Stub: decodes UTF-8 JSON bytes back to a DownloadRecord without decryption.
     *
     * TODO: Replace with AES-GCM decryption via CryptoKit.
     */
    override fun decrypt(data: ByteArray): DownloadRecord {
        // TODO: Decrypt via CryptoKit before deserializing.
        val json = data.decodeToString()
        return deserializeFromJson(json)
            ?: throw IllegalArgumentException("Failed to decrypt/deserialize record")
    }

    /**
     * Always returns true in the stub (no key required).
     *
     * TODO: Verify the Keychain key exists and is usable.
     */
    override fun isKeyValid(): Boolean {
        // TODO: Check Keychain for existing AES-GCM key.
        return true
    }

    /**
     * No-op in the stub.
     *
     * TODO: Delete the existing Keychain key and generate a fresh AES-256 key.
     * Note: Regenerating invalidates all previously encrypted cloud data.
     */
    override fun regenerateKey() {
        // TODO: Generate new AES-GCM key via CryptoKit and store in Keychain.
        // Warning: all previously uploaded encrypted data becomes unreadable.
    }

    // ---------------------------------------------------------------------------
    // Private helpers: minimal JSON serialization (matches IosCloudBackupRepository format)
    // ---------------------------------------------------------------------------

    private fun serializeToJson(record: DownloadRecord): String =
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

    private fun deserializeFromJson(json: String): DownloadRecord? {
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
                syncStatus = extractString(json, "syncStatus") ?: "NOT_SYNCED",
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun extractString(
        json: String,
        key: String,
    ): String? = Regex("\"$key\":\"([^\"]*)\"").find(json)?.groupValues?.get(1)

    private fun extractLong(
        json: String,
        key: String,
    ): Long? = Regex("\"$key\":(\\d+)").find(json)?.groupValues?.get(1)?.toLongOrNull()

    private fun String.escapeJson() = replace("\\", "\\\\").replace("\"", "\\\"")
}
