package com.socialvideodownloader

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.util.Log
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.ffmpeg.FFmpeg
import com.yausername.aria2c.Aria2c
import com.socialvideodownloader.core.domain.di.IoDispatcher
import com.socialvideodownloader.feature.download.service.DownloadNotificationManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class SocialVideoDownloaderApp : Application() {

    @Inject
    @IoDispatcher
    lateinit var ioDispatcher: kotlinx.coroutines.CoroutineDispatcher

    private val applicationScope = CoroutineScope(SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        initializeLibraries()
    }

    private fun createNotificationChannels() {
        val channel = NotificationChannel(
            DownloadNotificationManager.CHANNEL_ID,
            getString(R.string.download_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.download_notification_channel_description)
        }
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun initializeLibraries() {
        applicationScope.launch(ioDispatcher) {
            try {
                YoutubeDL.getInstance().init(this@SocialVideoDownloaderApp)
                Log.d(TAG, "YoutubeDL initialized successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize YoutubeDL", e)
            }

            try {
                FFmpeg.getInstance().init(this@SocialVideoDownloaderApp)
                Log.d(TAG, "FFmpeg initialized successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize FFmpeg", e)
            }

            try {
                Aria2c.getInstance().init(this@SocialVideoDownloaderApp)
                Log.d(TAG, "Aria2c initialized successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize Aria2c", e)
            }

            try {
                YoutubeDL.getInstance().updateYoutubeDL(
                    this@SocialVideoDownloaderApp,
                    YoutubeDL.UpdateChannel.NIGHTLY,
                )
                Log.d(TAG, "yt-dlp updated successfully")
            } catch (e: Exception) {
                Log.d(TAG, "yt-dlp update skipped: ${e.message}")
            }
        }
    }

    companion object {
        private const val TAG = "SocialVideoDownloaderApp"
    }
}
