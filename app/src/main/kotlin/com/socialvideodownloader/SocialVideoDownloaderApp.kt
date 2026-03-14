package com.socialvideodownloader

import android.app.Application
import android.util.Log
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.ffmpeg.FFmpeg
import com.yausername.aria2c.Aria2c
import com.socialvideodownloader.core.domain.di.IoDispatcher
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
        initializeLibraries()
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
        }
    }

    companion object {
        private const val TAG = "SocialVideoDownloaderApp"
    }
}
