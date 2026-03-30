package com.socialvideodownloader.shared.feature.history.di

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
import kotlinx.coroutines.CoroutineScope
import org.koin.dsl.module

/**
 * Koin module providing [SharedHistoryViewModel] and its local use cases.
 *
 * The VM is a factory — each screen instance owns its own CoroutineScope.
 *
 * Usage:
 * ```kotlin
 * val vm = getKoin().get<SharedHistoryViewModel> { parametersOf(myScope) }
 * ```
 */
val sharedHistoryModule =
    module {

        factory {
            DeleteHistoryItemUseCaseShared(
                repository = get<DownloadRepository>(),
                fileManager = get<FileAccessManager>(),
            )
        }

        factory { (scope: CoroutineScope) ->
            SharedHistoryViewModel(
                coroutineScope = scope,
                downloadRepository = get<DownloadRepository>(),
                fileManager = get<FileAccessManager>(),
                deleteHistoryItemUseCase = get<DeleteHistoryItemUseCaseShared>(),
                observeCloudCapacity = get<ObserveCloudCapacityUseCase>(),
                billingRepository = get<BillingRepository>(),
                enableCloudBackupUseCase = get<EnableCloudBackupUseCase>(),
                disableCloudBackupUseCase = get<DisableCloudBackupUseCase>(),
                syncManager = get<SyncManager>(),
                backupPreferences = get<BackupPreferences>(),
                restoreFromCloudUseCase = get<RestoreFromCloudUseCase>(),
                cloudAuthService = get<CloudAuthService>(),
                clipboard = get<PlatformClipboard>(),
            )
        }
    }
