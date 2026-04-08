package com.socialvideodownloader.core.data.remote

import android.content.Context
import com.socialvideodownloader.core.domain.di.IoDispatcher
import com.socialvideodownloader.core.domain.model.DownloadRequest
import com.socialvideodownloader.core.domain.model.VideoMetadata
import com.socialvideodownloader.core.domain.repository.VideoExtractorRepository
import com.socialvideodownloader.shared.network.auth.CookieStore
import com.socialvideodownloader.shared.network.auth.detectPlatform
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class VideoExtractorRepositoryImpl
    @Inject
    constructor(
        private val mapper: VideoInfoMapper,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
        @ApplicationContext private val context: Context,
        private val cookieStore: CookieStore,
    ) : VideoExtractorRepository {
        override suspend fun extractInfo(url: String): VideoMetadata =
            withContext(ioDispatcher) {
                val request = YoutubeDLRequest(url)
                // Inject stored platform cookies for authenticated extraction
                val cookieFile = writeCookieFile(url)
                if (cookieFile != null) {
                    request.addOption("--cookies", cookieFile.absolutePath)
                }
                try {
                    val videoInfo = YoutubeDL.getInstance().getInfo(request)
                    mapper.mapToMetadata(videoInfo, url)
                } finally {
                    cookieFile?.delete()
                }
            }

        private fun writeCookieFile(url: String): File? {
            val platform = detectPlatform(url) ?: return null
            val cookies = cookieStore.getCookies(platform) ?: return null
            val file = File.createTempFile("ytdlp_cookies_${platform.name}_", ".txt", context.cacheDir)
            file.writeText(cookies)
            return file
        }

        override suspend fun download(
            request: DownloadRequest,
            callback: (Float, Long, String) -> Unit,
        ): String =
            withContext(ioDispatcher) {
                val outputDir = File(context.cacheDir, "ytdl_downloads")
                outputDir.deleteRecursively()
                outputDir.mkdirs()

                val dlRequest = YoutubeDLRequest(request.sourceUrl)
                val formatString =
                    if (request.isVideoOnly) {
                        "${request.formatId}+bestaudio"
                    } else {
                        request.formatId
                    }
                dlRequest.addOption("-f", formatString)
                if (request.isVideoOnly) {
                    dlRequest.addOption("--merge-output-format", "mp4")
                }
                dlRequest.addOption("-o", File(outputDir, "%(title).200B.%(ext)s").absolutePath)

                val cookieFile = writeCookieFile(request.sourceUrl)
                if (cookieFile != null) {
                    dlRequest.addOption("--cookies", cookieFile.absolutePath)
                }
                try {
                    YoutubeDL.getInstance().execute(
                        request = dlRequest,
                        processId = request.id,
                    ) { progress, eta, line ->
                        callback(progress, eta, line ?: "")
                    }

                    outputDir.listFiles()?.firstOrNull()?.absolutePath
                        ?: throw IllegalStateException("Download completed but no output file found")
                } finally {
                    cookieFile?.delete()
                }
            }

        override fun cancelDownload(processId: String) {
            YoutubeDL.getInstance().destroyProcessById(processId)
        }
    }
