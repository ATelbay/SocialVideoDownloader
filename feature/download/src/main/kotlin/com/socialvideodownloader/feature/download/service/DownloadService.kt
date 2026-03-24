package com.socialvideodownloader.feature.download.service

import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.content.FileProvider
import com.socialvideodownloader.core.domain.di.IoDispatcher
import com.socialvideodownloader.core.domain.model.DownloadProgress
import com.socialvideodownloader.core.domain.model.DownloadRecord
import com.socialvideodownloader.core.domain.model.DownloadRequest
import com.socialvideodownloader.core.domain.model.DownloadStatus
import com.socialvideodownloader.core.domain.usecase.CancelDownloadUseCase
import com.socialvideodownloader.core.domain.usecase.DownloadVideoUseCase
import com.socialvideodownloader.core.domain.usecase.SaveDownloadRecordUseCase
import com.socialvideodownloader.core.domain.usecase.SaveFileToMediaStoreUseCase
import com.socialvideodownloader.feature.download.R
import com.socialvideodownloader.feature.download.ui.ErrorMessageMapper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@AndroidEntryPoint
class DownloadService : Service() {

    @Inject lateinit var downloadVideo: DownloadVideoUseCase
    @Inject lateinit var cancelDownload: CancelDownloadUseCase
    @Inject lateinit var saveDownloadRecord: SaveDownloadRecordUseCase
    @Inject lateinit var saveFileToMediaStore: SaveFileToMediaStoreUseCase
    @Inject lateinit var notificationManager: DownloadNotificationManager
    @Inject lateinit var stateHolder: DownloadServiceStateHolder
    @Inject @IoDispatcher lateinit var ioDispatcher: CoroutineDispatcher
    @Inject lateinit var errorMessageMapper: ErrorMessageMapper

    private val serviceScope = CoroutineScope(SupervisorJob())
    private val queue = ConcurrentLinkedQueue<DownloadRequest>()
    private val isProcessing = AtomicBoolean(false)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_DOWNLOAD -> {
                val request = extractDownloadRequest(intent) ?: return START_NOT_STICKY
                enqueueDownload(request)
            }
            ACTION_CANCEL_DOWNLOAD -> {
                val requestId = intent.getStringExtra(EXTRA_REQUEST_ID) ?: return START_NOT_STICKY
                cancelDownload(requestId)
                java.io.File(cacheDir, "ytdl_downloads").listFiles()?.forEach { it.deleteRecursively() }
                stateHolder.update(DownloadServiceState.Cancelled(requestId))
                stopIfQueueEmpty()
            }
        }
        return START_NOT_STICKY
    }

    private fun enqueueDownload(request: DownloadRequest) {
        queue.add(request)
        if (!isProcessing.get()) {
            processNext()
        } else {
            stateHolder.update(DownloadServiceState.Queued(queue.map { it.id }))
        }
    }

    private fun processNext() {
        val request = queue.poll() ?: run {
            isProcessing.set(false)
            stateHolder.update(DownloadServiceState.Idle)
            stopSelf()
            return
        }

        isProcessing.set(true)
        val notificationId = request.id.hashCode()

        val initialProgress = DownloadProgress(
            requestId = request.id,
            progressPercent = 0f,
            downloadedBytes = 0L,
            speedBytesPerSec = 0L,
            etaSeconds = 0L,
        )
        stateHolder.update(DownloadServiceState.Downloading(request.id, initialProgress))

        val foregroundNotification = notificationManager.buildProgressNotification(
            notificationId = notificationId,
            requestId = request.id,
            videoTitle = request.videoTitle,
            progressPercent = 0,
            speedText = "",
            etaText = "",
        )
        startForeground(notificationId, foregroundNotification)

        serviceScope.launch(ioDispatcher) {
            try {
                var highWaterMark = 0f
                val outputPath = downloadVideo(request) { progressPercent, etaSeconds, speedText ->
                    val speedBytes = parseSpeedToBytes(speedText)
                    val safeProgress = progressPercent.coerceAtLeast(0f)
                    val isMuxing = highWaterMark >= MUXING_DETECTION_THRESHOLD && safeProgress < highWaterMark
                    highWaterMark = maxOf(highWaterMark, safeProgress)
                    val displayProgress = if (isMuxing) 100f else safeProgress
                    val safeEta = etaSeconds.coerceAtLeast(0L)
                    val totalBytes = request.totalBytes
                    val progress = DownloadProgress(
                        requestId = request.id,
                        progressPercent = displayProgress,
                        downloadedBytes = if (!isMuxing && totalBytes != null && totalBytes > 0) {
                            ((displayProgress / 100f) * totalBytes).toLong()
                        } else {
                            0L
                        },
                        totalBytes = request.totalBytes,
                        speedBytesPerSec = if (isMuxing) 0L else speedBytes,
                        etaSeconds = if (isMuxing) 0L else safeEta,
                        isMuxing = isMuxing,
                    )
                    stateHolder.update(DownloadServiceState.Downloading(request.id, progress))

                    val updatedNotification = notificationManager.buildProgressNotification(
                        notificationId = notificationId,
                        requestId = request.id,
                        videoTitle = request.videoTitle,
                        progressPercent = if (isMuxing) 100 else displayProgress.toInt(),
                        speedText = if (isMuxing) "" else speedText,
                        etaText = if (isMuxing) "" else formatEta(safeEta),
                    )
                    notificationManager.updateNotification(notificationId, updatedNotification)
                }

                if (request.shareOnly) {
                    // Share mode: move to share temp dir, create FileProvider URI, skip MediaStore/Room
                    val shareDir = java.io.File(cacheDir, SHARE_TEMP_DIR)
                    shareDir.mkdirs()
                    val sourceFile = java.io.File(outputPath)
                    val destFile = java.io.File(shareDir, "${request.id}.${sourceFile.extension}")
                    if (!sourceFile.renameTo(destFile)) {
                        sourceFile.copyTo(destFile, overwrite = true)
                        sourceFile.delete()
                    }

                    val shareUri = FileProvider.getUriForFile(
                        this@DownloadService,
                        "$packageName.fileprovider",
                        destFile,
                    )

                    stateHolder.update(DownloadServiceState.Completed(request.id, shareUri.toString()))
                    notificationManager.cancelNotification(notificationId)
                } else {
                    // Normal mode: save to MediaStore and Room
                    val ext = java.io.File(outputPath).extension.ifEmpty { "mp4" }
                    val mimeType = if (request.isVideoOnly || ext == "mp4" || ext == "mkv" || ext == "webm") {
                        "video/$ext"
                    } else {
                        "audio/$ext"
                    }
                    val savedUri = saveFileToMediaStore(
                        outputPath,
                        "${request.videoTitle}.$ext",
                        mimeType,
                    )

                    val fileSizeBytes: Long? = try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            contentResolver.query(
                                android.net.Uri.parse(savedUri),
                                arrayOf(android.provider.OpenableColumns.SIZE),
                                null, null, null,
                            )?.use { cursor ->
                                if (cursor.moveToFirst()) cursor.getLong(0).takeIf { it > 0 } else null
                            }
                        } else {
                            java.io.File(savedUri).length().takeIf { it > 0 }
                        }
                    } catch (_: Exception) {
                        null
                    }

                    saveDownloadRecord(
                        DownloadRecord(
                            sourceUrl = request.sourceUrl,
                            videoTitle = request.videoTitle,
                            thumbnailUrl = request.thumbnailUrl,
                            formatLabel = request.formatLabel,
                            filePath = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) savedUri else null,
                            mediaStoreUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) savedUri else null,
                            fileSizeBytes = fileSizeBytes,
                            status = DownloadStatus.COMPLETED,
                            createdAt = System.currentTimeMillis(),
                            completedAt = System.currentTimeMillis(),
                        ),
                    )

                    stateHolder.update(DownloadServiceState.Completed(request.id, savedUri))
                    notificationManager.cancelNotification(notificationId)
                    notificationManager.showCompletionNotification(
                        notificationId xor COMPLETION_ID_XOR,
                        request.videoTitle,
                        mediaStoreUri = savedUri,
                        mimeType = mimeType,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Download failed for ${request.id}", e)
                val errorMsg = e.message ?: getString(R.string.notification_download_failed)
                val userFacingError = errorMessageMapper.map(e)
                stateHolder.update(DownloadServiceState.Failed(request.id, errorMsg))
                notificationManager.cancelNotification(notificationId)
                if (!request.shareOnly) {
                    notificationManager.showErrorNotification(
                        notificationId xor COMPLETION_ID_XOR,
                        request.videoTitle,
                        userFacingError,
                    )
                    saveDownloadRecord(
                        DownloadRecord(
                            sourceUrl = request.sourceUrl,
                            videoTitle = request.videoTitle,
                            thumbnailUrl = request.thumbnailUrl,
                            formatLabel = request.formatLabel,
                            filePath = null,
                            status = DownloadStatus.FAILED,
                            createdAt = System.currentTimeMillis(),
                        ),
                    )
                }
            } finally {
                if (queue.peek() == null) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                }
                processNext()
            }
        }
    }

    private fun stopIfQueueEmpty() {
        if (queue.isEmpty() && !isProcessing.get()) {
            stateHolder.update(DownloadServiceState.Idle)
            stopSelf()
        }
    }

    private fun parseSpeedToBytes(speedText: String): Long {
        return try {
            when {
                speedText.contains("MiB/s", ignoreCase = true) ->
                    (speedText.replace("MiB/s", "", ignoreCase = true).trim().toFloat() * 1024 * 1024).toLong()
                speedText.contains("KiB/s", ignoreCase = true) ->
                    (speedText.replace("KiB/s", "", ignoreCase = true).trim().toFloat() * 1024).toLong()
                else -> 0L
            }
        } catch (e: NumberFormatException) {
            0L
        }
    }

    private fun formatEta(etaSeconds: Long): String {
        val m = etaSeconds / 60
        val s = etaSeconds % 60
        return "%02d:%02d".format(m, s)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private fun extractDownloadRequest(intent: Intent): DownloadRequest? {
        val id = intent.getStringExtra(EXTRA_REQUEST_ID) ?: return null
        val sourceUrl = intent.getStringExtra(EXTRA_SOURCE_URL) ?: return null
        val videoTitle = intent.getStringExtra(EXTRA_VIDEO_TITLE) ?: return null
        val formatId = intent.getStringExtra(EXTRA_FORMAT_ID) ?: return null
        val formatLabel = intent.getStringExtra(EXTRA_FORMAT_LABEL) ?: return null
        return DownloadRequest(
            id = id,
            sourceUrl = sourceUrl,
            videoTitle = videoTitle,
            thumbnailUrl = intent.getStringExtra(EXTRA_THUMBNAIL_URL),
            formatId = formatId,
            formatLabel = formatLabel,
            isVideoOnly = intent.getBooleanExtra(EXTRA_IS_VIDEO_ONLY, false),
            shareOnly = intent.getBooleanExtra(EXTRA_SHARE_ONLY, false),
        )
    }

    companion object {
        private const val TAG = "DownloadService"
        private const val MUXING_DETECTION_THRESHOLD = 95f
        const val SHARE_TEMP_DIR = "ytdl_share"
        // XOR mask to derive completion/error notification IDs from progress IDs without collision.
        // hashCode() returns values in [-2^31, 2^31-1]; XOR with this bit pattern flips the sign bit,
        // guaranteeing the result is always in a different half of the Int space.
        private const val COMPLETION_ID_XOR = Int.MIN_VALUE  // 0x80000000

        const val ACTION_START_DOWNLOAD = "com.socialvideodownloader.action.START_DOWNLOAD"
        const val ACTION_CANCEL_DOWNLOAD = "com.socialvideodownloader.action.CANCEL_DOWNLOAD"
        const val EXTRA_REQUEST_ID = "extra_request_id"
        const val EXTRA_SOURCE_URL = "extra_source_url"
        const val EXTRA_VIDEO_TITLE = "extra_video_title"
        const val EXTRA_THUMBNAIL_URL = "extra_thumbnail_url"
        const val EXTRA_FORMAT_ID = "extra_format_id"
        const val EXTRA_FORMAT_LABEL = "extra_format_label"
        const val EXTRA_IS_VIDEO_ONLY = "extra_is_video_only"
        const val EXTRA_SHARE_ONLY = "extra_share_only"
    }
}
