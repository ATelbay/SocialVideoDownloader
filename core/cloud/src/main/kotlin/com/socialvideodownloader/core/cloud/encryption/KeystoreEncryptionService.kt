package com.socialvideodownloader.core.cloud.encryption

import android.util.Log
import com.socialvideodownloader.core.domain.model.DownloadRecord
import com.socialvideodownloader.core.domain.model.DownloadStatus
import com.socialvideodownloader.core.domain.sync.CloudAuthService
import com.socialvideodownloader.core.domain.sync.EncryptionService
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject

private const val TAG = "KeystoreEncryptionSvc"
private const val KEY_DERIVATION_SALT = "SocialVideoDownloader-backup-v1"
private const val OLD_KEYSTORE_ALIAS = "cloud_backup_key"

class KeyInvalidatedException(message: String) : Exception(message)

class KeystoreEncryptionService
    @Inject
    constructor(
        private val authService: CloudAuthService,
    ) : EncryptionService {
        init {
            // Clean up old Android Keystore entry if it exists (no longer used)
            try {
                val ks = KeyStore.getInstance("AndroidKeyStore").also { it.load(null) }
                if (ks.containsAlias(OLD_KEYSTORE_ALIAS)) {
                    ks.deleteEntry(OLD_KEYSTORE_ALIAS)
                    Log.d(TAG, "Deleted legacy Android Keystore entry")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to clean up legacy Keystore entry", e)
            }
        }

        // The derived key depends only on the (stable) UID, so cache it to avoid
        // re-running HMAC on every encrypt/decrypt — bulk restore decrypts up to the
        // tier limit (e.g. 1000) records in one pass. Recomputed when the UID changes.
        @Volatile
        private var cachedKey: Pair<String, SecretKeySpec>? = null

        /**
         * Derive a deterministic AES-256 key from the user's Firebase UID.
         * HMAC-SHA256(key=salt, data=uid) → 32 bytes → AES key.
         * Same user always gets the same key regardless of device or data clear.
         *
         * SECURITY NOTE — this is OBFUSCATION, not confidentiality.
         * The "salt" ([KEY_DERIVATION_SALT]) is a constant shipped in the APK, and the UID is
         * NOT secret — it is the Firestore document path (`users/{uid}`) stored next to the
         * encrypted payload. Anyone with read access to the cloud data (e.g. the project owner,
         * or via a Firestore-rules misconfiguration) can reproduce this key. So the encryption
         * protects against casual at-rest inspection only, NOT against a party who can already
         * read the stored documents.
         *
         * This tradeoff is intentional: a deterministic, UID-derived key is what lets a user
         * restore their backup on a new device (or after clearing app data) with zero extra
         * input. Real confidentiality would require a user-held secret (passphrase), which breaks
         * existing backups, needs new UI on both platforms, and makes "forgot passphrase" mean
         * permanent backup loss — tracked as a separate product decision, not a code-only fix.
         */
        private fun deriveKey(): SecretKeySpec {
            val uid =
                authService.getCurrentUid()
                    ?: error("Not authenticated — sign in before encrypting")
            cachedKey?.let { (cachedUid, key) -> if (cachedUid == uid) return key }
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(KEY_DERIVATION_SALT.toByteArray(Charsets.UTF_8), "HmacSHA256"))
            val keyBytes = mac.doFinal(uid.toByteArray(Charsets.UTF_8))
            return SecretKeySpec(keyBytes, "AES").also { cachedKey = uid to it }
        }

        override fun encrypt(record: DownloadRecord): ByteArray {
            val key = deriveKey()
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv
            val plaintext = recordToJson(record).toByteArray(Charsets.UTF_8)
            val ciphertext = cipher.doFinal(plaintext)
            // Prepend IV (12 bytes) to ciphertext
            return iv + ciphertext
        }

        override fun decrypt(data: ByteArray): DownloadRecord {
            val key = deriveKey()
            val iv = data.copyOfRange(0, 12)
            val ciphertext = data.copyOfRange(12, data.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
            val plaintext = cipher.doFinal(ciphertext)
            return jsonToRecord(String(plaintext, Charsets.UTF_8))
        }

        override fun isKeyValid(): Boolean = authService.isAuthenticated()

        override fun regenerateKey() {
            // Key is derived from UID — nothing to regenerate.
            // Old Keystore-based keys are cleaned up in init{}.
        }

        private fun recordToJson(record: DownloadRecord): String {
            return JSONObject().apply {
                put("sourceUrl", record.sourceUrl)
                put("videoTitle", record.videoTitle)
                record.thumbnailUrl?.let { put("thumbnailUrl", it) }
                put("formatLabel", record.formatLabel)
                put("status", record.status.name)
                put("createdAt", record.createdAt)
                record.completedAt?.let { put("completedAt", it) }
                record.fileSizeBytes?.let { put("fileSizeBytes", it) }
            }.toString()
        }

        private fun jsonToRecord(json: String): DownloadRecord {
            val obj = JSONObject(json)
            return DownloadRecord(
                sourceUrl = obj.getString("sourceUrl"),
                videoTitle = obj.getString("videoTitle"),
                thumbnailUrl = obj.optString("thumbnailUrl").takeIf { it.isNotEmpty() },
                formatLabel = obj.optString("formatLabel", ""),
                status = DownloadStatus.entries.find { it.name == obj.getString("status") } ?: DownloadStatus.FAILED,
                createdAt = obj.getLong("createdAt"),
                completedAt = if (obj.has("completedAt")) obj.getLong("completedAt") else null,
                fileSizeBytes = if (obj.has("fileSizeBytes")) obj.getLong("fileSizeBytes") else null,
            )
        }
    }
