package com.socialvideodownloader.core.domain.sync

import com.socialvideodownloader.core.domain.repository.CloudBackupRepository
import com.socialvideodownloader.core.domain.repository.DownloadRepository
import kotlinx.coroutines.flow.first

data class RestoreResult(
    val restored: Int,
    val skipped: Int,
    val failed: Int,
    val error: String? = null,
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
                return RestoreResult(restored = 0, skipped = 0, failed = 0, error = e.message)
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
                    downloadRepository.insert(record)
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
