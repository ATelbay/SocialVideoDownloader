package com.socialvideodownloader.shared.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

actual fun createHttpClient(): HttpClient =
    HttpClient(Darwin) {
        engine {
            configureRequest {
                setTimeoutInterval(ServerConfig.readTimeoutSeconds.toDouble())
            }
        }
        install(HttpTimeout) {
            connectTimeoutMillis = ServerConfig.connectTimeoutSeconds * 1000
            requestTimeoutMillis = ServerConfig.readTimeoutSeconds * 1000
        }
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                },
            )
        }
        install(WebSockets) {
            pingIntervalMillis = 30_000
        }
    }
