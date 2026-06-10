package com.socialvideodownloader.core.domain.sync

import com.socialvideodownloader.core.domain.repository.DownloadRepository

class EnableCloudBackupUseCase(
    private val authService: CloudAuthService,
    private val preferences: BackupPreferences,
    private val syncManager: SyncManager,
    private val downloadRepository: DownloadRepository,
) {
    /**
     * Enable cloud backup.
     *
     * @param idToken Google credential to sign in with. Pass `null` when the user is already
     *   authenticated (e.g. re-enabling the toggle after a previous sign-in) to skip sign-in
     *   while still running the first-enable backfill and pending-sync flush.
     */
    suspend operator fun invoke(idToken: String? = null) {
        if (idToken != null) {
            authService.signInWithGoogleCredential(idToken)
        }
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
