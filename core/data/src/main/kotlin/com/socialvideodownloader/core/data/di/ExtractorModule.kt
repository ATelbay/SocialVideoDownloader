package com.socialvideodownloader.core.data.di

import com.socialvideodownloader.core.data.local.MediaStoreRepositoryImpl
import com.socialvideodownloader.core.data.remote.FallbackVideoExtractorRepository
import com.socialvideodownloader.core.domain.repository.MediaStoreRepository
import com.socialvideodownloader.core.domain.repository.VideoExtractorRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ExtractorModule {

    @Binds
    @Singleton
    abstract fun bindVideoExtractorRepository(
        impl: FallbackVideoExtractorRepository,
    ): VideoExtractorRepository

    @Binds
    @Singleton
    abstract fun bindMediaStoreRepository(
        impl: MediaStoreRepositoryImpl,
    ): MediaStoreRepository

}
