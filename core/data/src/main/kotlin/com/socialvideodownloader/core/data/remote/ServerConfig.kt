package com.socialvideodownloader.core.data.remote

import com.socialvideodownloader.core.data.BuildConfig

object ServerConfig {
    val BASE_URL: String = BuildConfig.YTDLP_SERVER_URL
    val EXTRACT_API_KEY: String = BuildConfig.YTDLP_API_KEY
    const val EXTRACT_PATH = "/extract"
    const val CONNECT_TIMEOUT_SECONDS = 10L
    const val READ_TIMEOUT_SECONDS = 60L
}
