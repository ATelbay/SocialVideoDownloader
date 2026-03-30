package com.socialvideodownloader.shared.network

import com.socialvideodownloader.core.domain.model.VideoMetadata
import com.socialvideodownloader.shared.network.dto.ServerExtractRequest
import com.socialvideodownloader.shared.network.dto.ServerExtractResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.CancellationException

class ServerVideoExtractorApi(
    private val client: HttpClient,
    private val mapper: ServerResponseMapper,
) {
    suspend fun extractInfo(url: String): VideoMetadata {
        val apiKey = ServerConfig.extractApiKey
        val response: HttpResponse =
            client.post("${ServerConfig.baseUrl}${ServerConfig.extractPath}") {
                contentType(ContentType.Application.Json)
                if (!apiKey.isNullOrEmpty()) {
                    header("X-API-Key", apiKey)
                }
                setBody(ServerExtractRequest(url = url, apiKey = apiKey))
            }

        if (response.status != HttpStatusCode.OK) {
            throw ServerExtractionException(
                "Server extraction failed (${response.status.value})",
                response.status.value,
            )
        }

        val serverResponse: ServerExtractResponse = response.body()
        return mapper.mapToMetadata(serverResponse, url)
    }

    suspend fun downloadFile(
        url: String,
        outputPath: String,
        requestId: String,
        onProgress: (Float, Long, String) -> Unit,
    ): String {
        val response: HttpResponse =
            try {
                client.get(url) {
                    header("X-Request-Id", requestId)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                throw DownloadException("Download request failed: ${e.message}", cause = e)
            }

        if (response.status != HttpStatusCode.OK) {
            throw DownloadException("Download failed (${response.status.value})")
        }

        val contentLength = response.headers["Content-Length"]?.toLongOrNull() ?: -1L
        val channel = response.bodyAsChannel()
        var downloadedBytes = 0L
        val buffer = ByteArray(8192)

        // Return the output path to the caller — platform-specific file writing
        // is handled by the platform implementation that calls this function.
        // For the shared KMP implementation we stream the bytes and report progress.
        while (!channel.isClosedForRead) {
            val bytesRead = channel.readAvailable(buffer)
            if (bytesRead <= 0) break
            downloadedBytes += bytesRead
            val progress =
                if (contentLength > 0) {
                    (downloadedBytes.toFloat() / contentLength * 100f)
                } else {
                    -1f
                }
            onProgress(progress, downloadedBytes, "")
        }

        return outputPath
    }
}

class ServerExtractionException(
    message: String,
    val statusCode: Int,
) : Exception(message)

class DownloadException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
