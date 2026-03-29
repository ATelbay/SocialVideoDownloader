package com.socialvideodownloader.core.data.remote

import android.content.Context
import android.util.Log
import com.socialvideodownloader.core.domain.di.IoDispatcher
import com.socialvideodownloader.core.domain.model.DownloadRequest
import com.socialvideodownloader.core.domain.model.VideoMetadata
import com.socialvideodownloader.core.domain.repository.VideoExtractorRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class FallbackVideoExtractorRepository @Inject constructor(
    private val local: VideoExtractorRepositoryImpl,
    private val serverApi: ServerVideoExtractorApi,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @ApplicationContext private val context: Context,
) : VideoExtractorRepository {

    override suspend fun extractInfo(url: String): VideoMetadata {
        return try {
            local.extractInfo(url)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.w(TAG, "Local extraction failed, trying server", e)
            withContext(ioDispatcher) {
                serverApi.extractInfo(url)
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
                val safeTitle = request.videoTitle
                    .replace(Regex("[^a-zA-Z0-9._\\-]"), "_")
                    .take(200)
                val ext = request.formatId.substringAfterLast('.', "mp4")
                serverApi.downloadFile(
                    url = directUrl,
                    outputDir = outputDir,
                    fileName = "$safeTitle.$ext",
                    requestId = request.id,
                    onProgress = callback,
                )
            }
        } else {
            local.download(request, callback)
        }
    }

    override fun cancelDownload(processId: String) {
        local.cancelDownload(processId)
        serverApi.cancelDownload(processId)
    }

    companion object {
        private const val TAG = "FallbackExtractor"
    }
}
