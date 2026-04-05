@file:OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)

package com.socialvideodownloader.shared.network

import com.socialvideodownloader.core.domain.model.VideoMetadata
import com.socialvideodownloader.shared.network.auth.CookieStore
import com.socialvideodownloader.shared.network.auth.NetscapeCookieParser
import com.socialvideodownloader.shared.network.auth.SupportedPlatform
import com.socialvideodownloader.shared.network.auth.detectPlatform
import com.socialvideodownloader.shared.network.dto.ServerExtractResponse
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.headers
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.HttpMethod
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class WebSocketExtractorApi(
    private val client: HttpClient,
    private val mapper: ServerResponseMapper,
    private val secureCookieStore: CookieStore,
) {
    // Separate plain client for executing proxied HTTP requests — no ContentNegotiation needed.
    private val rawClient = HttpClient { }

    suspend fun extractViaProxy(url: String): VideoMetadata {
        val wsUrl =
            ServerConfig.baseUrl
                .replace("https://", "wss://")
                .replace("http://", "ws://")

        var result: VideoMetadata? = null
        var extractionError: ServerExtractionException? = null

        client.webSocket(urlString = "$wsUrl/ws/extract") {
            // Build initial extraction request with optional cookies
            val initFrame =
                buildJsonObject {
                    put("type", "extract_request")
                    put("url", url)
                    // Include cookies for pre-seeding yt-dlp's cookie jar
                    val platform = detectPlatform(url)
                    if (platform != null) {
                        val cookies = secureCookieStore.getCookies(platform)
                        if (cookies != null) {
                            put("cookies", Base64.Default.encode(cookies.encodeToByteArray()))
                        }
                    }
                }.toString()
            send(Frame.Text(initFrame))

            for (frame in incoming) {
                if (frame !is Frame.Text) continue

                val text = frame.readText()
                val json = Json.parseToJsonElement(text).jsonObject
                val type = json["type"]?.jsonPrimitive?.content ?: continue

                when (type) {
                    "http_request" -> {
                        val reqId = json["id"]?.jsonPrimitive?.content ?: continue
                        val reqUrl = json["url"]?.jsonPrimitive?.content ?: continue
                        val reqMethod = json["method"]?.jsonPrimitive?.content ?: "GET"
                        val reqHeaders =
                            json["headers"]?.jsonObject
                                ?.mapValues { it.value.jsonPrimitive.content }
                                ?: emptyMap()
                        val reqBody = json["body"]?.jsonPrimitive?.contentOrNull

                        val responseFrame =
                            try {
                                // Inject platform cookies into proxied request headers
                                val mergedHeaders = reqHeaders.toMutableMap()
                                val reqHost =
                                    try {
                                        reqUrl.substringAfter("://").substringBefore("/").substringBefore(":").lowercase()
                                    } catch (_: Exception) {
                                        ""
                                    }

                                val matchedPlatform =
                                    SupportedPlatform.entries.firstOrNull { platform ->
                                        platform.hostMatches.any { host -> reqHost == host || reqHost.endsWith(".$host") }
                                    }
                                if (matchedPlatform != null) {
                                    val cookies = secureCookieStore.getCookies(matchedPlatform)
                                    if (cookies != null) {
                                        val pairs = NetscapeCookieParser.parseToNameValuePairs(cookies)
                                        if (pairs.isNotEmpty()) {
                                            val cookieString = pairs.joinToString("; ") { "${it.first}=${it.second}" }
                                            val existing = mergedHeaders["Cookie"] ?: mergedHeaders["cookie"] ?: ""
                                            mergedHeaders["Cookie"] =
                                                if (existing.isNotBlank()) {
                                                    "$existing; $cookieString"
                                                } else {
                                                    cookieString
                                                }
                                        }
                                    }
                                }

                                val response =
                                    rawClient.request(reqUrl) {
                                        method = HttpMethod.parse(reqMethod)
                                        headers {
                                            mergedHeaders.forEach { (k, v) -> append(k, v) }
                                        }
                                        if (reqBody != null) {
                                            setBody(Base64.Default.decode(reqBody))
                                        }
                                    }
                                val responseBody = Base64.Default.encode(response.bodyAsBytes())
                                val finalUrl = response.call.request.url.toString()

                                buildJsonObject {
                                    put("type", "http_response")
                                    put("id", reqId)
                                    put("status", response.status.value)
                                    put("url", finalUrl)
                                    put("body", responseBody)
                                    put(
                                        "headers",
                                        buildJsonArray {
                                            response.headers.entries().forEach { (k, values) ->
                                                values.forEach { v ->
                                                    add(
                                                        buildJsonArray {
                                                            add(k)
                                                            add(v)
                                                        },
                                                    )
                                                }
                                            }
                                        },
                                    )
                                }
                            } catch (e: Exception) {
                                buildJsonObject {
                                    put("type", "http_error")
                                    put("id", reqId)
                                    put("error", e.message ?: "Unknown error")
                                }
                            }
                        send(Frame.Text(responseFrame.toString()))
                    }

                    "extract_result" -> {
                        val data = json["data"] ?: continue
                        val serverResponse =
                            Json {
                                ignoreUnknownKeys = true
                                isLenient = true
                            }.decodeFromJsonElement(ServerExtractResponse.serializer(), data)
                        result = mapper.mapToMetadata(serverResponse, url)
                        break
                    }

                    "extract_error" -> {
                        val detail =
                            json["detail"]?.jsonPrimitive?.content
                                ?: "WebSocket extraction error"
                        extractionError = ServerExtractionException(detail, 422)
                        break
                    }
                }
            }
        }

        return result
            ?: throw (extractionError ?: ServerExtractionException("No result received from WebSocket", 500))
    }
}
