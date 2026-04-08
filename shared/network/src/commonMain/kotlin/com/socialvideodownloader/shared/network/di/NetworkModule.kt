package com.socialvideodownloader.shared.network.di

import com.socialvideodownloader.shared.network.ServerResponseMapper
import com.socialvideodownloader.shared.network.ServerVideoExtractorApi
import com.socialvideodownloader.shared.network.WebSocketExtractorApi
import com.socialvideodownloader.shared.network.createHttpClient
import org.koin.dsl.module

val networkModule =
    module {
        single { createHttpClient() }
        single { ServerResponseMapper() }
        single { ServerVideoExtractorApi(get(), get(), get()) }
        single { WebSocketExtractorApi(get(), get(), get()) }
    }
