package com.socialvideodownloader.core.data.di

import com.socialvideodownloader.shared.network.ServerResponseMapper
import com.socialvideodownloader.shared.network.ServerVideoExtractorApi
import com.socialvideodownloader.shared.network.WebSocketExtractorApi
import com.socialvideodownloader.shared.network.auth.CookieStore
import com.socialvideodownloader.shared.network.createHttpClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import javax.inject.Singleton

// NOTE: These Hilt bindings are intentionally separate from the Koin networkModule in
// :shared:network. FallbackVideoExtractorRepository uses @Inject constructor and receives
// HttpClient and ServerVideoExtractorApi from the Hilt graph. The KoinBridgeModule does
// not bridge network types, so removing these would break Android compilation.
// TODO: Tech debt — consider bridging network types through KoinBridgeModule to eliminate
//   this duplication, or migrate FallbackVideoExtractorRepository to use Koin directly.
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
        cookieStore: CookieStore,
    ): ServerVideoExtractorApi = ServerVideoExtractorApi(client, mapper, cookieStore)

    @Provides
    @Singleton
    fun provideWebSocketExtractorApi(
        client: HttpClient,
        mapper: ServerResponseMapper,
        cookieStore: CookieStore,
    ): WebSocketExtractorApi = WebSocketExtractorApi(client, mapper, cookieStore)
}
