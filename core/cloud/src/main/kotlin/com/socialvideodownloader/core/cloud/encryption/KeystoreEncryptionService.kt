package com.socialvideodownloader.core.cloud.encryption

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.util.Log
import com.socialvideodownloader.core.domain.model.DownloadRecord
import com.socialvideodownloader.core.domain.model.DownloadStatus
import com.socialvideodownloader.core.domain.sync.EncryptionService
import org.json.JSONObject
import java.security.KeyStore
import java.security.KeyStoreException
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject

private const val TAG = "KeystoreEncryptionSvc"

class KeyInvalidatedException(message: String) : Exception(message)

class KeystoreEncryptionService @Inject constructor() : EncryptionService {

    private val keyAlias = "cloud_backup_key"
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").also { it.load(null) }

    override fun encrypt(record: DownloadRecord): ByteArray {
        return try {
            val key = getOrCreateKey()
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv
            val plaintext = recordToJson(record).toByteArray(Charsets.UTF_8)
            val ciphertext = cipher.doFinal(plaintext)
            // Prepend IV (12 bytes) to ciphertext
            iv + ciphertext
        } catch (e: KeyPermanentlyInvalidatedException) {
            Log.w(TAG, "Key permanently invalidated during encrypt — regenerating key", e)
            handleKeyInvalidation()
        } catch (e: KeyStoreException) {
            Log.w(TAG, "KeyStore exception during encrypt — regenerating key", e)
            handleKeyInvalidation()
        } catch (e: AEADBadTagException) {
            Log.e(TAG, "AEADBadTag during encrypt", e)
            throw KeyInvalidatedException("Encryption failed: data integrity check failed")
        }
    }

    override fun decrypt(data: ByteArray): DownloadRecord {
        return try {
            val key = getOrCreateKey()
            val iv = data.copyOfRange(0, 12)
            val ciphertext = data.copyOfRange(12, data.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
            val plaintext = cipher.doFinal(ciphertext)
            jsonToRecord(String(plaintext, Charsets.UTF_8))
        } catch (e: KeyPermanentlyInvalidatedException) {
            Log.w(TAG, "Key permanently invalidated during decrypt — old records are unrecoverable", e)
            regenerateKey()
            throw KeyInvalidatedException(
                "Decryption key was invalidated. Old cloud records are unrecoverable. " +
                    "A new key has been generated for future backups.",
            )
        } catch (e: KeyStoreException) {
            Log.w(TAG, "KeyStore exception during decrypt — key may be invalidated", e)
            regenerateKey()
            throw KeyInvalidatedException(
                "Decryption key error. Old cloud records are unrecoverable. " +
                    "A new key has been generated for future backups.",
            )
        } catch (e: AEADBadTagException) {
            Log.e(TAG, "AEADBadTag during decrypt — data corrupted or wrong key", e)
            throw KeyInvalidatedException("Decryption failed: data integrity check failed")
        }
    }

    override fun isKeyValid(): Boolean {
        return try {
            val key = keyStore.getKey(keyAlias, null) as? SecretKey ?: return false
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key)
            true
        } catch (_: KeyPermanentlyInvalidatedException) {
            false
        } catch (_: Exception) {
            false
        }
    }

    override fun regenerateKey() {
        if (keyStore.containsAlias(keyAlias)) {
            keyStore.deleteEntry(keyAlias)
        }
        createKey()
    }

    private fun handleKeyInvalidation(): Nothing {
        regenerateKey()
        throw KeyInvalidatedException(
            "Encryption key was permanently invalidated. Old cloud records are unrecoverable. " +
                "A new key has been generated for future backups.",
        )
    }

    private fun getOrCreateKey(): SecretKey {
        val existing = keyStore.getKey(keyAlias, null) as? SecretKey
        return existing ?: createKey()
    }

    private fun createKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val spec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
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
