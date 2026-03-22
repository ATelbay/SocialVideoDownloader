package com.socialvideodownloader.core.domain.sync

import javax.inject.Inject

class DisableCloudBackupUseCase
    @Inject
    constructor(
        private val preferences: BackupPreferences,
        private val authService: CloudAuthService,
    ) {
        suspend operator fun invoke() {
            preferences.setBackupEnabled(false)
            authService.signOut()
        }
    }
