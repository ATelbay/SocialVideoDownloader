package com.socialvideodownloader.core.data.remote

import android.content.Context
import android.util.Log
import com.socialvideodownloader.core.data.media.StreamMuxer
import com.socialvideodownloader.core.domain.di.IoDispatcher
import com.socialvideodownloader.core.domain.model.DownloadRequest
import com.socialvideodownloader.core.domain.model.VideoMetadata
import com.socialvideodownloader.core.domain.repository.VideoExtractorRepository
import com.socialvideodownloader.shared.network.ServerExtractionException
import com.socialvideodownloader.shared.network.ServerVideoExtractorApi
import com.socialvideodownloader.shared.network.WebSocketExtractorApi
import com.socialvideodownloader.shared.network.auth.SupportedPlatform
import com.socialvideodownloader.shared.network.auth.detectPlatform
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class FallbackVideoExtractorRepository
    @Inject
    constructor(
        private val local: VideoExtractorRepositoryImpl,
        private val wsApi: WebSocketExtractorApi,
        private val serverApi: ServerVideoExtractorApi,
        private val httpClient: HttpClient,
        private val streamMuxer: StreamMuxer,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
        @ApplicationContext private val context: Context,
    ) : VideoExtractorRepository {
        override suspend fun extractInfo(url: String): VideoMetadata {
            // YouTube requires a JS runtime (node/deno) for signature solving,
            // which is unavailable on Android — skip straight to server.
            if (detectPlatform(url) == SupportedPlatform.YOUTUBE) {
                return extractViaServer(url)
            }

            return try {
                local.extractInfo(url)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.w(TAG, "Local extraction failed, trying server", e)
                extractViaServer(url)
            }
        }

        private suspend fun extractViaServer(url: String): VideoMetadata {
            return try {
                wsApi.extractViaProxy(url)
            } catch (wsError: Exception) {
                if (wsError is CancellationException) throw wsError
                Log.w(TAG, "WS proxy failed, trying REST", wsError)
                try {
                    serverApi.extractInfo(url)
                } catch (restError: Exception) {
                    if (restError is CancellationException) throw restError
                    Log.w(TAG, "REST extraction also failed", restError)
                    if (wsError is ServerExtractionException) throw wsError
                    throw restError
                }
            }
        }

        override suspend fun download(
            request: DownloadRequest,
            callback: (Float, Long, String) -> Unit,
        ): String {
            val directUrl = request.directDownloadUrl
            return if (directUrl != null) {
                withContext(ioDispatcher) {
                    val outputDir = File(context.cacheDir, "ytdl_downloads")
                    outputDir.mkdirs()
                    val safeTitle =
                        request.videoTitle
                            .replace(Regex("[^a-zA-Z0-9._\\-]"), "_")
                            .take(200)
                    val ext = request.ext.ifBlank { "mp4" }
                    val outputFile = File(outputDir, "$safeTitle.$ext")

                    val audioUrl = request.audioDirectUrl
                    if (audioUrl != null) {
                        downloadAndMux(directUrl, audioUrl, outputFile, ext, request.id, callback)
                    } else {
                        downloadToFile(
                            url = directUrl,
                            outputFile = outputFile,
                            requestId = request.id,
                            onProgress = callback,
                        )
                    }
                }
            } else {
                local.download(request, callback)
            }
        }

        /**
         * Video-only DASH stream + separate audio stream: fetch both over HTTP and merge them
         * on-device. yt-dlp's `+bestaudio` merge never runs on this path, so without this step
         * the saved file would have no audio track.
         */
        private suspend fun downloadAndMux(
            videoUrl: String,
            audioUrl: String,
            outputFile: File,
            containerExt: String,
            requestId: String,
            onProgress: (Float, Long, String) -> Unit,
        ): String {
            val videoTemp = File(outputFile.parentFile, "${outputFile.nameWithoutExtension}.video.$containerExt")
            val audioTemp = File(outputFile.parentFile, "${outputFile.nameWithoutExtension}.audio.$containerExt")
            return try {
                downloadToFile(videoUrl, videoTemp, requestId) { progress, eta, line ->
                    onProgress(scaleProgress(progress, 0f, VIDEO_PROGRESS_END), eta, line)
                }
                downloadToFile(audioUrl, audioTemp, requestId) { progress, eta, line ->
                    onProgress(scaleProgress(progress, VIDEO_PROGRESS_END, AUDIO_PROGRESS_END), eta, line)
                }
                // Third param is a yt-dlp speed string on the local path; keep it empty here.
                onProgress(AUDIO_PROGRESS_END, 0L, "")
                try {
                    streamMuxer.mux(videoTemp, audioTemp, outputFile, containerExt)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // Keep the video-only stream rather than failing the whole download —
                    // this matches the pre-mux behavior for devices/codecs MediaMuxer rejects.
                    Log.w(TAG, "On-device mux failed, saving video without audio", e)
                    outputFile.delete()
                    if (!videoTemp.renameTo(outputFile)) {
                        videoTemp.copyTo(outputFile, overwrite = true)
                    }
                }
                onProgress(100f, 0L, "")
                outputFile.absolutePath
            } catch (e: Exception) {
                outputFile.delete()
                throw e
            } finally {
                videoTemp.delete()
                audioTemp.delete()
            }
        }

        private fun scaleProgress(
            progress: Float,
            start: Float,
            end: Float,
        ): Float =
            if (progress < 0f) {
                start
            } else {
                start + (progress.coerceAtMost(100f) / 100f) * (end - start)
            }

        private suspend fun downloadToFile(
            url: String,
            outputFile: File,
            requestId: String,
            onProgress: (Float, Long, String) -> Unit,
        ): String {
            try {
                val response =
                    httpClient.get(url) {
                        header("X-Request-Id", requestId)
                    }

                if (response.status != HttpStatusCode.OK) {
                    throw IllegalStateException("Download failed (${response.status.value})")
                }

                val contentLength = response.headers["Content-Length"]?.toLongOrNull() ?: -1L
                val channel = response.bodyAsChannel()
                var downloadedBytes = 0L
                val buffer = ByteArray(8192)

                outputFile.outputStream().use { outputStream ->
                    while (!channel.isClosedForRead) {
                        val bytesRead = channel.readAvailable(buffer)
                        if (bytesRead <= 0) break
                        outputStream.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        val progress =
                            if (contentLength > 0) {
                                (downloadedBytes.toFloat() / contentLength * 100f)
                            } else {
                                -1f
                            }
                        onProgress(progress, 0L, "")
                    }
                }

                return outputFile.absolutePath
            } catch (e: Exception) {
                outputFile.delete()
                throw e
            }
        }

        override fun cancelDownload(processId: String) {
            local.cancelDownload(processId)
        }

        companion object {
            private const val TAG = "FallbackExtractor"

            // Progress segments for the two-stream download: video 0-85%, audio 85-95%,
            // then muxing finishes at 100%. Monotonic so the service's progress-drop
            // muxing heuristic never falsely triggers.
            private const val VIDEO_PROGRESS_END = 85f
            private const val AUDIO_PROGRESS_END = 95f
        }
    }
