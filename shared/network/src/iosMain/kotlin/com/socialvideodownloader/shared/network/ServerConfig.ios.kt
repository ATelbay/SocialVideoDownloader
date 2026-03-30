package com.socialvideodownloader.shared.network

import platform.Foundation.NSBundle

actual object ServerConfig {
    actual val baseUrl: String
        get() =
            NSBundle.mainBundle.objectForInfoDictionaryKey("YTDLP_SERVER_URL") as? String
                ?: ""
    actual val extractApiKey: String?
        get() =
            (NSBundle.mainBundle.objectForInfoDictionaryKey("YTDLP_API_KEY") as? String)
                ?.takeIf { it.isNotEmpty() }
    actual val extractPath: String = "/extract"
    actual val connectTimeoutSeconds: Long = 10L
    actual val readTimeoutSeconds: Long = 60L
}
