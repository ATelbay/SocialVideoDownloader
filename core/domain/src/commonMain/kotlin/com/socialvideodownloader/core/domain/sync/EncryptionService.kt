package com.socialvideodownloader.core.domain.sync

import com.socialvideodownloader.core.domain.model.DownloadRecord

interface EncryptionService {
    /** Encrypt a DownloadRecord to a byte array. */
    fun encrypt(record: DownloadRecord): ByteArray

    /** Decrypt a byte array back to a DownloadRecord. */
    fun decrypt(data: ByteArray): DownloadRecord

    /** Check if the encryption key is valid and usable. */
    fun isKeyValid(): Boolean

    /** Regenerate the encryption key (invalidates old encrypted data). */
    fun regenerateKey()
}
