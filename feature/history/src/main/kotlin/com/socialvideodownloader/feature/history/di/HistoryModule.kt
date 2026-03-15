package com.socialvideodownloader.feature.history.di

import com.socialvideodownloader.feature.history.file.AndroidHistoryFileManager
import com.socialvideodownloader.feature.history.file.HistoryFileManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class HistoryModule {
    @Binds
    abstract fun bindHistoryFileManager(impl: AndroidHistoryFileManager): HistoryFileManager
}
