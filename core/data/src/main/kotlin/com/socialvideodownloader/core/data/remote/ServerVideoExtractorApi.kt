package com.socialvideodownloader.core.data.remote

import com.socialvideodownloader.core.data.remote.dto.ServerExtractRequest
import com.socialvideodownloader.core.data.remote.dto.ServerExtractResponse
import com.socialvideodownloader.core.domain.model.VideoMetadata
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServerVideoExtractorApi @Inject constructor(
    private val client: OkHttpClient,
    private val mapper: ServerResponseMapper,
) {

    private val json = Json { ignoreUnknownKeys = true }

    fun extractInfo(url: String): VideoMetadata {
        val requestBody = json.encodeToString(ServerExtractRequest(url = url))
            .toRequestBody("application/json".toMediaType())

        val requestBuilder = Request.Builder()
            .url("${ServerConfig.BASE_URL}${ServerConfig.EXTRACT_PATH}")
            .post(requestBody)
        if (ServerConfig.EXTRACT_API_KEY.isNotEmpty()) {
            requestBuilder.addHeader("X-API-Key", ServerConfig.EXTRACT_API_KEY)
        }
        val request = requestBuilder.build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            val errorBody = response.body?.string() ?: ""
            throw IOException("Server extraction failed (${response.code}): $errorBody")
        }

        val body = response.body?.string()
            ?: throw IOException("Empty response from server")
        val serverResponse = json.decodeFromString<ServerExtractResponse>(body)
        return mapper.mapToMetadata(serverResponse, url)
    }

    fun downloadFile(
        url: String,
        outputDir: File,
        fileName: String,
        requestId: String,
        onProgress: (Float, Long, String) -> Unit,
    ): String {
        outputDir.mkdirs()
        val outputFile = File(outputDir, fileName)

        val request = Request.Builder()
            .url(url)
            .tag(requestId)
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw IOException("Download failed (${response.code})")
        }

        val body = response.body ?: throw IOException("Empty download response")
        val totalBytes = body.contentLength()
        var downloadedBytes = 0L

        body.byteStream().use { input ->
            outputFile.outputStream().use { output ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead
                    val progress = if (totalBytes > 0) {
                        (downloadedBytes.toFloat() / totalBytes * 100f)
                    } else {
                        -1f
                    }
                    onProgress(progress, 0L, "")
                }
            }
        }

        return outputFile.absolutePath
    }

    fun cancelDownload(requestId: String) {
        client.dispatcher.queuedCalls()
            .filter { it.request().tag() == requestId }
            .forEach { it.cancel() }
        client.dispatcher.runningCalls()
            .filter { it.request().tag() == requestId }
            .forEach { it.cancel() }
    }
}
