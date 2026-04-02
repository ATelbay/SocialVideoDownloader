package com.socialvideodownloader.shared.network

import com.socialvideodownloader.core.domain.model.VideoMetadata
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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class WebSocketExtractorApi(
    private val client: HttpClient,
    private val mapper: ServerResponseMapper,
) {
    // Separate plain client for executing proxied HTTP requests — no ContentNegotiation needed.
    private val rawClient = HttpClient { }

    @OptIn(ExperimentalEncodingApi::class)
    suspend fun extractViaProxy(url: String): VideoMetadata {
        val wsUrl = ServerConfig.baseUrl
            .replace("https://", "wss://")
            .replace("http://", "ws://")

        var result: VideoMetadata? = null
        var extractionError: ServerExtractionException? = null

        client.webSocket(urlString = "$wsUrl/ws/extract") {
            // Send initial extraction request
            val initFrame = buildJsonObject {
                put("type", "extract_request")
                put("url", url)
            }.toString()
            send(Frame.Text(initFrame))

            for (frame in incoming) {
                if (frame !is Frame.Text) continue

                val text = frame.readText()
                val json = Json.parseToJsonElement(text).jsonObject
                val type = json["type"]?.jsonPrimitive?.content ?: continue

                when (type) {
                    "http_request" -> {
                        val reqUrl = json["url"]?.jsonPrimitive?.content ?: continue
                        val reqMethod = json["method"]?.jsonPrimitive?.content ?: "GET"
                        val reqHeaders = json["headers"]?.jsonObject
                            ?.mapValues { it.value.jsonPrimitive.content }
                            ?: emptyMap()
                        val reqBody = json["body"]?.jsonPrimitive?.content

                        val responseFrame = try {
                            val response = rawClient.request(reqUrl) {
                                method = HttpMethod.parse(reqMethod)
                                headers {
                                    reqHeaders.forEach { (k, v) -> append(k, v) }
                                }
                                if (reqBody != null) {
                                    setBody(Base64.Default.decode(reqBody))
                                }
                            }
                            val responseBody = Base64.Default.encode(response.bodyAsBytes())
                            val finalUrl = response.request.url.toString()
                            val responseHeaders = response.headers.entries()
                                .associate { it.key to it.value.first() }

                            buildJsonObject {
                                put("type", "http_response")
                                put("status", response.status.value)
                                put("url", finalUrl)
                                put("body", responseBody)
                                putJsonObject("headers") {
                                    responseHeaders.forEach { (k, v) -> put(k, v) }
                                }
                            }
                        } catch (e: Exception) {
                            buildJsonObject {
                                put("type", "http_error")
                                put("error", e.message ?: "Unknown error")
                            }
                        }
                        send(Frame.Text(responseFrame.toString()))
                    }

                    "extract_result" -> {
                        val data = json["data"] ?: continue
                        val serverResponse = Json {
                            ignoreUnknownKeys = true
                            isLenient = true
                        }.decodeFromJsonElement(ServerExtractResponse.serializer(), data)
                        result = mapper.mapToMetadata(serverResponse, url)
                        break
                    }

                    "extract_error" -> {
                        val detail = json["detail"]?.jsonPrimitive?.content
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
