package com.socialvideodownloader.core.domain.sync

import javax.inject.Inject

class DisableCloudBackupUseCase @Inject constructor(
    private val preferences: BackupPreferences,
) {
    suspend operator fun invoke() {
        preferences.setBackupEnabled(false)
    }
}
