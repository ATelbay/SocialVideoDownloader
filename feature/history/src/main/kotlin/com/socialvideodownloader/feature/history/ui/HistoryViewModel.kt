package com.socialvideodownloader.feature.history.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.socialvideodownloader.core.domain.file.FileAccessManager
import com.socialvideodownloader.core.domain.repository.BillingRepository
import com.socialvideodownloader.core.domain.repository.DownloadRepository
import com.socialvideodownloader.core.domain.sync.BackupPreferences
import com.socialvideodownloader.core.domain.sync.CloudAuthService
import com.socialvideodownloader.core.domain.sync.DisableCloudBackupUseCase
import com.socialvideodownloader.core.domain.sync.EnableCloudBackupUseCase
import com.socialvideodownloader.core.domain.sync.ObserveCloudCapacityUseCase
import com.socialvideodownloader.core.domain.sync.RestoreFromCloudUseCase
import com.socialvideodownloader.core.domain.sync.SyncManager
import com.socialvideodownloader.shared.data.platform.PlatformClipboard
import com.socialvideodownloader.shared.feature.history.DeleteHistoryItemUseCaseShared
import com.socialvideodownloader.shared.feature.history.SharedHistoryViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * Android thin delegate ViewModel for the history screen.
 *
 * All business logic lives in [SharedHistoryViewModel]. This class bridges
 * Hilt DI into the shared KMP ViewModel.
 *
 * The Compose UI layer continues to observe [uiState], [effect], and
 * [cloudBackupState] without any changes — the public API surface is
 * identical to the old ViewModel.
 *
 * Note: [launchPurchaseFlow] is kept here (not in shared VM) because
 * the Billing flow requires an Android Activity reference.
 */
@HiltViewModel
class HistoryViewModel
    @Inject
    constructor(
        private val downloadRepository: DownloadRepository,
        private val fileManager: FileAccessManager,
        private val observeCloudCapacity: ObserveCloudCapacityUseCase,
        private val billingRepository: BillingRepository,
        private val enableCloudBackupUseCase: EnableCloudBackupUseCase,
        private val disableCloudBackupUseCase: DisableCloudBackupUseCase,
        private val syncManager: SyncManager,
        private val backupPreferences: BackupPreferences,
        private val restoreFromCloudUseCase: RestoreFromCloudUseCase,
        private val cloudAuthService: CloudAuthService,
        private val clipboard: PlatformClipboard,
    ) : ViewModel() {
        private val shared =
            SharedHistoryViewModel(
                coroutineScope = viewModelScope,
                downloadRepository = downloadRepository,
                fileManager = fileManager,
                deleteHistoryItemUseCase =
                    DeleteHistoryItemUseCaseShared(
                        repository = downloadRepository,
                        fileManager = fileManager,
                    ),
                observeCloudCapacity = observeCloudCapacity,
                billingRepository = billingRepository,
                enableCloudBackupUseCase = enableCloudBackupUseCase,
                disableCloudBackupUseCase = disableCloudBackupUseCase,
                syncManager = syncManager,
                backupPreferences = backupPreferences,
                restoreFromCloudUseCase = restoreFromCloudUseCase,
                cloudAuthService = cloudAuthService,
                clipboard = clipboard,
            )

        val uiState: StateFlow<HistoryUiState> = shared.uiState
        val effect: SharedFlow<HistoryEffect> = shared.effect
        val cloudBackupState: StateFlow<CloudBackupState> = shared.cloudBackupState

        fun onIntent(intent: HistoryIntent) = shared.onIntent(intent)

        /** Called by the screen with the Activity reference required by Google Play Billing. */
        suspend fun launchPurchaseFlow(activity: Any) {
            billingRepository.launchPurchaseFlow(activity)
        }
    }
