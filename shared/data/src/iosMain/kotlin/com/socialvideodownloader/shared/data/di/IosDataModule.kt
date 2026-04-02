package com.socialvideodownloader.shared.data.di

import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import com.socialvideodownloader.core.domain.file.FileAccessManager
import com.socialvideodownloader.core.domain.repository.BillingRepository
import com.socialvideodownloader.core.domain.repository.CloudBackupRepository
import com.socialvideodownloader.core.domain.repository.DownloadRepository
import com.socialvideodownloader.core.domain.repository.VideoExtractorRepository
import com.socialvideodownloader.core.domain.sync.BackupPreferences
import com.socialvideodownloader.core.domain.sync.CloudAuthService
import com.socialvideodownloader.core.domain.sync.DisableCloudBackupUseCase
import com.socialvideodownloader.core.domain.sync.EnableCloudBackupUseCase
import com.socialvideodownloader.core.domain.sync.EncryptionService
import com.socialvideodownloader.core.domain.sync.ObserveCloudCapacityUseCase
import com.socialvideodownloader.core.domain.sync.RestoreFromCloudUseCase
import com.socialvideodownloader.core.domain.sync.SyncManager
import com.socialvideodownloader.core.domain.usecase.ExtractVideoInfoUseCase
import com.socialvideodownloader.core.domain.usecase.FindExistingDownloadUseCase
import com.socialvideodownloader.shared.data.billing.StoreKitBillingRepository
import com.socialvideodownloader.shared.data.billing.StubBillingProvider
import com.socialvideodownloader.shared.data.cloud.IosBackupPreferences
import com.socialvideodownloader.shared.data.cloud.IosCloudAuthService
import com.socialvideodownloader.shared.data.cloud.IosCloudBackupRepository
import com.socialvideodownloader.shared.data.cloud.IosConnectivityObserver
import com.socialvideodownloader.shared.data.cloud.IosEncryptionService
import com.socialvideodownloader.shared.data.cloud.IosSyncManager
import com.socialvideodownloader.shared.data.cloud.StubAuthProvider
import com.socialvideodownloader.shared.data.cloud.StubConnectivityProvider
import com.socialvideodownloader.shared.data.cloud.StubFirestoreProvider
import com.socialvideodownloader.shared.data.platform.IosClipboard
import com.socialvideodownloader.shared.data.platform.IosDownloadManager
import com.socialvideodownloader.shared.data.platform.IosFileAccessManager
import com.socialvideodownloader.shared.data.platform.IosFileStorage
import com.socialvideodownloader.shared.data.platform.IosStringProvider
import com.socialvideodownloader.shared.data.platform.PlatformClipboard
import com.socialvideodownloader.shared.data.platform.PlatformDownloadManager
import com.socialvideodownloader.shared.data.platform.PlatformFileStorage
import com.socialvideodownloader.shared.data.platform.PlatformStringProvider
import com.socialvideodownloader.shared.data.repository.ServerOnlyVideoExtractorRepository
import org.koin.dsl.module
import platform.Foundation.NSUserDefaults

/**
 * iOS-specific Koin module providing platform implementations.
 *
 * Binds all platform abstractions to their iOS implementations.
 * Phase 6: core platform implementations (download, file, clipboard, strings).
 * Phase 10: cloud backup, billing, and authentication implementations.
 *
 * Cloud and billing implementations currently use stub providers that are safe
 * for development. Replace stub providers with real Swift implementations by:
 *   1. Implementing `FirebaseAuthProvider` in Swift, register via `KoinHelper.registerAuthProvider()`
 *   2. Implementing `StoreKitBillingProvider` in Swift, register via `KoinHelper.registerBillingProvider()`
 *   3. Implementing `NWPathConnectivityProvider` in Swift, register via `KoinHelper.registerConnectivityProvider()`
 */
val iosDataModule =
    module {

        // -------------------------------------------------------------------------
        // Phase 6: Core platform implementations
        // -------------------------------------------------------------------------

        // iOS always uses the server API for video extraction — no local yt-dlp.
        single<VideoExtractorRepository> {
            ServerOnlyVideoExtractorRepository(serverApi = get(), wsApi = get())
        }

        single<PlatformDownloadManager> { IosDownloadManager() }
        single<PlatformFileStorage> { IosFileStorage() }
        single<PlatformClipboard> { IosClipboard() }
        single<PlatformStringProvider> { IosStringProvider() }

        // FileAccessManager + shared use cases — needed by shared feature ViewModels
        single<FileAccessManager> { IosFileAccessManager() }
        single { ExtractVideoInfoUseCase(repository = get()) }
        single { FindExistingDownloadUseCase(downloadRepository = get(), fileAccessManager = get()) }

        // -------------------------------------------------------------------------
        // Phase 10: Cloud backup, billing, and authentication
        // -------------------------------------------------------------------------

        // NSUserDefaults-backed Settings for preferences storage
        single<Settings> { NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults) }

        // BackupPreferences: persists cloud backup toggle + last sync timestamp
        single<BackupPreferences> { IosBackupPreferences(settings = get()) }

        // EncryptionService: stub — replace with CryptoKit-backed implementation in Xcode
        single<EncryptionService> { IosEncryptionService() }

        // Auth: stub provider until Firebase CocoaPod is integrated
        // TODO: Replace StubAuthProvider with FirebaseAuthProvider implemented in Swift
        single { StubAuthProvider() }
        single<CloudAuthService> { IosCloudAuthService(platformAuthProvider = get<StubAuthProvider>()) }

        // Connectivity observer: stub always-online until NWPathMonitor is wired
        // TODO: Replace StubConnectivityProvider with NWPathConnectivityProvider in Swift
        single { StubConnectivityProvider() }
        single {
            IosConnectivityObserver(connectivityProvider = get<StubConnectivityProvider>())
        }

        // Firestore cloud backup: stub Firestore provider until Firebase CocoaPod is integrated
        // TODO: Replace StubFirestoreProvider with a real FirestoreProvider implemented in Swift
        single<CloudBackupRepository> {
            IosCloudBackupRepository(
                firestoreProvider = StubFirestoreProvider(),
                encryptionService = get(),
            )
        }

        // SyncManager: processes upload/delete queue against Firestore
        single<SyncManager> {
            IosSyncManager(
                syncQueueDao = get(),
                cloudBackupRepository = get(),
                cloudAuthService = get(),
                backupPreferences = get(),
                connectivityObserver = get(),
            )
        }

        // Billing: stub StoreKit provider until App Store Connect products are configured
        // TODO: Replace StubBillingProvider with StoreKitBillingProvider implemented in Swift
        single { StubBillingProvider() }
        single<BillingRepository> { StoreKitBillingRepository(billingProvider = get<StubBillingProvider>()) }

        // -------------------------------------------------------------------------
        // Cloud use cases — domain logic wired with iOS platform implementations
        // -------------------------------------------------------------------------

        single {
            EnableCloudBackupUseCase(
                authService = get<CloudAuthService>(),
                preferences = get<BackupPreferences>(),
                syncManager = get<SyncManager>(),
                downloadRepository = get<DownloadRepository>(),
            )
        }

        single {
            DisableCloudBackupUseCase(
                preferences = get<BackupPreferences>(),
                authService = get<CloudAuthService>(),
            )
        }

        single {
            ObserveCloudCapacityUseCase(
                cloudBackupRepository = get<CloudBackupRepository>(),
                billingRepository = get<BillingRepository>(),
            )
        }

        single {
            RestoreFromCloudUseCase(
                cloudBackupRepository = get<CloudBackupRepository>(),
                downloadRepository = get<DownloadRepository>(),
            )
        }
    }
