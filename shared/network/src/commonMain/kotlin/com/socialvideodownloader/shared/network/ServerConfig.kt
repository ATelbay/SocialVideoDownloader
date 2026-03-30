package com.socialvideodownloader.shared.network

expect object ServerConfig {
    val baseUrl: String
    val extractApiKey: String?
    val extractPath: String
    val connectTimeoutSeconds: Long
    val readTimeoutSeconds: Long
}
