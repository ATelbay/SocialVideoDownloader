package com.socialvideodownloader.di

import com.socialvideodownloader.core.domain.file.FileAccessManager
import com.socialvideodownloader.core.domain.repository.BillingRepository
import com.socialvideodownloader.core.domain.repository.CloudBackupRepository
import com.socialvideodownloader.core.domain.repository.DownloadRepository
import com.socialvideodownloader.core.domain.repository.MediaStoreRepository
import com.socialvideodownloader.core.domain.repository.VideoExtractorRepository
import com.socialvideodownloader.core.domain.sync.BackupPreferences
import com.socialvideodownloader.core.domain.sync.CloudAuthService
import com.socialvideodownloader.core.domain.sync.DisableCloudBackupUseCase
import com.socialvideodownloader.core.domain.sync.EnableCloudBackupUseCase
import com.socialvideodownloader.core.domain.sync.ObserveCloudCapacityUseCase
import com.socialvideodownloader.core.domain.sync.RestoreFromCloudUseCase
import com.socialvideodownloader.core.domain.sync.SyncManager
import com.socialvideodownloader.core.domain.usecase.CancelDownloadUseCase
import com.socialvideodownloader.core.domain.usecase.DownloadVideoUseCase
import com.socialvideodownloader.core.domain.usecase.ExtractVideoInfoUseCase
import com.socialvideodownloader.core.domain.usecase.FindExistingDownloadUseCase
import com.socialvideodownloader.core.domain.usecase.SaveDownloadRecordUseCase
import com.socialvideodownloader.core.domain.usecase.SaveFileToMediaStoreUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt module providing domain use cases from :core:domain.
 *
 * After the KMP migration, use cases no longer carry @Inject annotations
 * (javax.inject is not available in commonMain). This module bridges the gap
 * by explicitly constructing each use case for Hilt's DI graph.
 */
@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {
    @Provides
    fun provideExtractVideoInfoUseCase(repository: VideoExtractorRepository): ExtractVideoInfoUseCase = ExtractVideoInfoUseCase(repository)

    @Provides
    fun provideDownloadVideoUseCase(repository: VideoExtractorRepository): DownloadVideoUseCase = DownloadVideoUseCase(repository)

    @Provides
    fun provideCancelDownloadUseCase(repository: VideoExtractorRepository): CancelDownloadUseCase = CancelDownloadUseCase(repository)

    @Provides
    fun provideSaveFileToMediaStoreUseCase(repository: MediaStoreRepository): SaveFileToMediaStoreUseCase =
        SaveFileToMediaStoreUseCase(repository)

    @Provides
    fun provideFindExistingDownloadUseCase(
        downloadRepository: DownloadRepository,
        fileAccessManager: FileAccessManager,
    ): FindExistingDownloadUseCase = FindExistingDownloadUseCase(downloadRepository, fileAccessManager)

    @Provides
    fun provideSaveDownloadRecordUseCase(repository: DownloadRepository): SaveDownloadRecordUseCase = SaveDownloadRecordUseCase(repository)

    @Provides
    fun provideDisableCloudBackupUseCase(
        preferences: BackupPreferences,
        authService: CloudAuthService,
    ): DisableCloudBackupUseCase = DisableCloudBackupUseCase(preferences, authService)

    @Provides
    fun provideEnableCloudBackupUseCase(
        authService: CloudAuthService,
        preferences: BackupPreferences,
        syncManager: SyncManager,
        downloadRepository: DownloadRepository,
    ): EnableCloudBackupUseCase = EnableCloudBackupUseCase(authService, preferences, syncManager, downloadRepository)

    @Provides
    fun provideObserveCloudCapacityUseCase(
        cloudBackupRepository: CloudBackupRepository,
        billingRepository: BillingRepository,
    ): ObserveCloudCapacityUseCase = ObserveCloudCapacityUseCase(cloudBackupRepository, billingRepository)

    @Provides
    fun provideRestoreFromCloudUseCase(
        cloudBackupRepository: CloudBackupRepository,
        downloadRepository: DownloadRepository,
    ): RestoreFromCloudUseCase = RestoreFromCloudUseCase(cloudBackupRepository, downloadRepository)
}
