package com.socialvideodownloader.core.cloud.di

import com.socialvideodownloader.core.cloud.auth.FirebaseCloudAuthService
import com.socialvideodownloader.core.cloud.encryption.KeystoreEncryptionService
import com.socialvideodownloader.core.cloud.preferences.CloudBackupPreferences
import com.socialvideodownloader.core.cloud.repository.FirestoreCloudBackupRepository
import com.socialvideodownloader.core.cloud.sync.FirestoreSyncManager
import com.socialvideodownloader.core.domain.repository.CloudBackupRepository
import com.socialvideodownloader.core.domain.sync.BackupPreferences
import com.socialvideodownloader.core.domain.sync.CloudAuthService
import com.socialvideodownloader.core.domain.sync.EncryptionService
import com.socialvideodownloader.core.domain.sync.SyncManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CloudModule {
    @Binds
    @Singleton
    abstract fun bindCloudAuthService(impl: FirebaseCloudAuthService): CloudAuthService

    @Binds
    @Singleton
    abstract fun bindEncryptionService(impl: KeystoreEncryptionService): EncryptionService

    @Binds
    @Singleton
    abstract fun bindCloudBackupRepository(impl: FirestoreCloudBackupRepository): CloudBackupRepository

    @Binds
    @Singleton
    abstract fun bindBackupPreferences(impl: CloudBackupPreferences): BackupPreferences

    @Binds
    @Singleton
    abstract fun bindSyncManager(impl: FirestoreSyncManager): SyncManager
}
