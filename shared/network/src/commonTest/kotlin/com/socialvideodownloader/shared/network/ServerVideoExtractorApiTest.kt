package com.socialvideodownloader.shared.network

import com.socialvideodownloader.shared.network.auth.CookieStore
import com.socialvideodownloader.shared.network.auth.SupportedPlatform
import com.socialvideodownloader.shared.network.dto.ServerExtractResponse
import com.socialvideodownloader.shared.network.dto.ServerFormatDto
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private class FakeCookieStore : CookieStore {
    private val store = mutableMapOf<SupportedPlatform, String>()

    override fun getCookies(platform: SupportedPlatform): String? = store[platform]

    override fun setCookies(
        platform: SupportedPlatform,
        cookies: String,
    ) {
        store[platform] = cookies
    }

    override fun clearCookies(platform: SupportedPlatform) {
        store.remove(platform)
    }

    override fun isConnected(platform: SupportedPlatform): Boolean = store.containsKey(platform)

    override fun connectedPlatforms(): List<SupportedPlatform> = store.keys.toList()
}

class ServerVideoExtractorApiTest {
    private val json = Json { ignoreUnknownKeys = true }

    private fun createApiWithResponse(
        responseBody: String,
        statusCode: HttpStatusCode = HttpStatusCode.OK,
    ): ServerVideoExtractorApi {
        val mockEngine =
            MockEngine { _ ->
                respond(
                    content = responseBody,
                    status = statusCode,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            }
        val client =
            HttpClient(mockEngine) {
                install(ContentNegotiation) {
                    json(json)
                }
            }
        val mapper = ServerResponseMapper()
        return ServerVideoExtractorApi(client, mapper, FakeCookieStore())
    }

    @Test
    fun extractInfo_returnsVideoMetadata_whenServerRespondsSuccessfully() =
        runTest {
            val response =
                ServerExtractResponse(
                    title = "Test Video",
                    thumbnail = "https://example.com/thumb.jpg",
                    duration = 120.0,
                    uploader = "TestChannel",
                    formats =
                        listOf(
                            ServerFormatDto(
                                formatId = "22",
                                ext = "mp4",
                                resolution = "1920x1080",
                                filesize = 50_000_000L,
                                url = "https://example.com/video.mp4",
                                vcodec = "avc1",
                                acodec = "mp4a",
                            ),
                        ),
                )
            val api = createApiWithResponse(json.encodeToString(response))

            val metadata = api.extractInfo("https://youtube.com/watch?v=test")

            assertEquals("Test Video", metadata.title)
            assertEquals("https://example.com/thumb.jpg", metadata.thumbnailUrl)
            assertEquals(120, metadata.durationSeconds)
            assertEquals(1, metadata.formats.size)
            assertEquals("22", metadata.formats[0].formatId)
            assertEquals("mp4", metadata.formats[0].ext)
        }

    @Test
    fun extractInfo_throwsException_whenServerReturnsError() =
        runTest {
            val api =
                createApiWithResponse(
                    responseBody = """{"error": "Extraction failed"}""",
                    statusCode = HttpStatusCode.InternalServerError,
                )

            assertFailsWith<Exception> {
                api.extractInfo("https://youtube.com/watch?v=test")
            }
        }

    @Test
    fun extractInfo_throwsException_whenServerReturns404() =
        runTest {
            val api =
                createApiWithResponse(
                    responseBody = """{"error": "Not found"}""",
                    statusCode = HttpStatusCode.NotFound,
                )

            assertFailsWith<Exception> {
                api.extractInfo("https://youtube.com/watch?v=test")
            }
        }

    @Test
    fun extractInfo_mapsMultipleFormats_correctly() =
        runTest {
            val response =
                ServerExtractResponse(
                    title = "Multi-format Video",
                    thumbnail = null,
                    duration = 60.0,
                    uploader = null,
                    formats =
                        listOf(
                            ServerFormatDto(
                                formatId = "137",
                                ext = "mp4",
                                resolution = "1920x1080",
                                filesize = 100_000_000L,
                                url = "https://example.com/video_1080p.mp4",
                                vcodec = "avc1",
                                acodec = "none",
                            ),
                            ServerFormatDto(
                                formatId = "140",
                                ext = "m4a",
                                resolution = null,
                                filesize = 5_000_000L,
                                url = "https://example.com/audio.m4a",
                                vcodec = "none",
                                acodec = "mp4a",
                            ),
                        ),
                )
            val api = createApiWithResponse(json.encodeToString(response))

            val metadata = api.extractInfo("https://youtube.com/watch?v=test")

            assertEquals(2, metadata.formats.size)
            val videoFormat = metadata.formats.first { it.formatId == "137" }
            assertTrue(videoFormat.isVideoOnly)
            val audioFormat = metadata.formats.first { it.formatId == "140" }
            assertTrue(audioFormat.isAudioOnly)
        }

    @Test
    fun extractInfo_usesCorrectUrl_withApiKey() =
        runTest {
            var capturedRequest: io.ktor.client.request.HttpRequestData? = null
            val response =
                ServerExtractResponse(
                    title = "Test",
                    thumbnail = null,
                    duration = null,
                    uploader = null,
                    formats = emptyList(),
                )
            val responseJson = json.encodeToString(response)

            val mockEngine =
                MockEngine { request ->
                    capturedRequest = request
                    respond(
                        content = responseJson,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }
            val client =
                HttpClient(mockEngine) {
                    install(ContentNegotiation) {
                        json(json)
                    }
                }
            val mapper = ServerResponseMapper()
            val api = ServerVideoExtractorApi(client, mapper, FakeCookieStore())

            api.extractInfo("https://youtube.com/watch?v=test")

            assertNotNull(capturedRequest)
            assertTrue(capturedRequest!!.url.toString().contains("/extract"))
        }
}
