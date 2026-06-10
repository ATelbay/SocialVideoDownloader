package com.socialvideodownloader.core.domain.sync

import com.socialvideodownloader.core.domain.repository.CloudBackupRepository
import com.socialvideodownloader.core.domain.repository.DownloadRepository
import kotlinx.coroutines.flow.first

/**
 * Why the restore could not complete. Typed so the UI can localize the message instead of
 * surfacing a raw, non-localized exception string. Classification lives here — the single place
 * that owns the failure — rather than being re-derived by string matching in the UI layer.
 */
enum class RestoreErrorReason {
    /** The cloud records exist but cannot be decrypted (the encryption key is unavailable). */
    KEY_UNAVAILABLE,

    /** Any other failure (network, store read, etc.). */
    GENERIC,
}

data class RestoreResult(
    val restored: Int,
    val skipped: Int,
    val failed: Int,
    val errorReason: RestoreErrorReason? = null,
)

class RestoreFromCloudUseCase(
    private val cloudBackupRepository: CloudBackupRepository,
    private val downloadRepository: DownloadRepository,
) {
    suspend operator fun invoke(onProgress: (current: Int, total: Int) -> Unit = { _, _ -> }): RestoreResult {
        val cloudRecords =
            try {
                cloudBackupRepository.fetchAllRecords()
            } catch (e: Exception) {
                // The repository surfaces decryption-key loss through the exception message; map it to
                // a typed reason here so callers never have to inspect free-text error strings.
                val reason =
                    if (e.message?.contains("key", ignoreCase = true) == true) {
                        RestoreErrorReason.KEY_UNAVAILABLE
                    } else {
                        RestoreErrorReason.GENERIC
                    }
                return RestoreResult(restored = 0, skipped = 0, failed = 0, errorReason = reason)
            }

        val total = cloudRecords.size
        val localRecords = downloadRepository.getAll().first()
        val localKeys =
            localRecords
                .map { it.sourceUrl to it.createdAt }
                .toHashSet()

        var restored = 0
        var skipped = 0
        var failed = 0

        cloudRecords.forEachIndexed { index, record ->
            val key = record.sourceUrl to record.createdAt
            if (localKeys.contains(key)) {
                skipped++
            } else {
                try {
                    // These records already exist in the cloud — mark them SYNCED so the repository
                    // does not re-enqueue them for upload (avoids redundant traffic and counter churn).
                    downloadRepository.insert(record.copy(syncStatus = "SYNCED"))
                    localKeys.add(key)
                    restored++
                } catch (e: Exception) {
                    failed++
                }
            }
            onProgress(index + 1, total)
        }

        return RestoreResult(restored = restored, skipped = skipped, failed = failed)
    }
}
