package com.socialvideodownloader.shared.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit

actual fun createHttpClient(): HttpClient =
    HttpClient(OkHttp) {
        engine {
            config {
                connectTimeout(ServerConfig.connectTimeoutSeconds, TimeUnit.SECONDS)
                readTimeout(ServerConfig.readTimeoutSeconds, TimeUnit.SECONDS)
            }
        }
        install(WebSockets) {
            pingIntervalMillis = 30_000
        }
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                },
            )
        }
    }
