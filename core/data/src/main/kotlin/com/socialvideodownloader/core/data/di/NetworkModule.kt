package com.socialvideodownloader.core.data.di

import com.socialvideodownloader.shared.network.ServerResponseMapper
import com.socialvideodownloader.shared.network.ServerVideoExtractorApi
import com.socialvideodownloader.shared.network.createHttpClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideHttpClient(): HttpClient = createHttpClient()

    @Provides
    @Singleton
    fun provideServerResponseMapper(): ServerResponseMapper = ServerResponseMapper()

    @Provides
    @Singleton
    fun provideServerVideoExtractorApi(
        client: HttpClient,
        mapper: ServerResponseMapper,
    ): ServerVideoExtractorApi = ServerVideoExtractorApi(client, mapper)
}
