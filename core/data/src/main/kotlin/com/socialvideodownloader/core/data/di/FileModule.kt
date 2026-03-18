package com.socialvideodownloader.core.data.di

import com.socialvideodownloader.core.data.file.AndroidFileAccessManager
import com.socialvideodownloader.core.domain.file.FileAccessManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class FileModule {
    @Binds
    abstract fun bindFileAccessManager(impl: AndroidFileAccessManager): FileAccessManager
}
