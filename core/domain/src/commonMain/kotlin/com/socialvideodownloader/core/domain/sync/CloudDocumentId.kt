package com.socialvideodownloader.core.domain.sync

import org.kotlincrypto.hash.sha2.SHA256

/**
 * Deterministic cloud document id for a backup record, shared by every platform.
 *
 * Lowercase-hex SHA-256 of `"$sourceUrl$createdAt"` (no separator) — byte-identical
 * to Android's historical `java.security.MessageDigest("SHA-256")` output, so existing
 * Android cloud backups keep the same ids. iOS previously used a different (FNV-1a)
 * scheme; aligning both on this function makes the same account map to the same
 * documents across devices (idempotent upload, hash-based delete, capacity counter).
 *
 * All call sites (upload, delete, restore) MUST use this so they agree on the id.
 */
fun cloudDocumentId(
    sourceUrl: String,
    createdAt: Long,
): String {
    val digest = SHA256().digest("$sourceUrl$createdAt".encodeToByteArray())
    return digest.joinToString("") { byte ->
        (byte.toInt() and 0xFF).toString(16).padStart(2, '0')
    }
}
