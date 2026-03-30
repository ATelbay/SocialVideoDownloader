package com.socialvideodownloader.core.domain.sync

import com.socialvideodownloader.core.domain.repository.DownloadRepository

class EnableCloudBackupUseCase(
    private val authService: CloudAuthService,
    private val preferences: BackupPreferences,
    private val syncManager: SyncManager,
    private val downloadRepository: DownloadRepository,
) {
    suspend operator fun invoke(idToken: String) {
        authService.signInWithGoogleCredential(idToken)
        val isFirstEnable = !preferences.hasEverEnabled()
        preferences.setBackupEnabled(true)
        preferences.setHasEverEnabled(true)
        if (isFirstEnable) {
            // Backfill: queue all existing completed downloads for upload
            val existing = downloadRepository.getCompletedSnapshot()
            for (record in existing) {
                syncManager.syncNewRecord(record)
            }
        }
        syncManager.processPendingOperations()
    }
}
