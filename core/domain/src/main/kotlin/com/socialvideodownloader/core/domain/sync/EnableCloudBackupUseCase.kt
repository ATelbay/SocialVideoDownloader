package com.socialvideodownloader.core.domain.sync

import kotlinx.coroutines.flow.first
import javax.inject.Inject

class EnableCloudBackupUseCase
    @Inject
    constructor(
        private val authService: CloudAuthService,
        private val preferences: BackupPreferences,
        private val syncManager: SyncManager,
    ) {
        suspend operator fun invoke() {
            val alreadyEnabled = preferences.observeIsBackupEnabled().first()
            if (preferences.hasEverEnabled() && alreadyEnabled) return

            authService.signInAnonymously()
            preferences.setBackupEnabled(true)
            preferences.setHasEverEnabled(true)
            syncManager.processPendingOperations()
        }
    }
