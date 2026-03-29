package com.socialvideodownloader.shared.network

actual object ServerConfig {
    actual val baseUrl: String = BuildConfig.YTDLP_SERVER_URL
    actual val extractApiKey: String? = BuildConfig.YTDLP_API_KEY.takeIf { it.isNotEmpty() }
    actual val extractPath: String = "/extract"
    actual val connectTimeoutSeconds: Long = 10L
    actual val readTimeoutSeconds: Long = 60L
}
