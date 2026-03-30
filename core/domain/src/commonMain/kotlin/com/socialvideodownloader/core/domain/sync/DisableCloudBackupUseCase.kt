package com.socialvideodownloader.core.domain.sync

class DisableCloudBackupUseCase(
    private val preferences: BackupPreferences,
    private val authService: CloudAuthService,
) {
    suspend operator fun invoke() {
        preferences.setBackupEnabled(false)
        authService.signOut()
    }
}
