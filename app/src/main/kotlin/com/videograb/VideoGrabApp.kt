package com.videograb

import android.app.Application
import android.util.Log
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.ffmpeg.FFmpeg
import com.yausername.aria2c.Aria2c
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@HiltAndroidApp
class VideoGrabApp : Application() {

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
                YoutubeDL.getInstance().init(this@VideoGrabApp)
                Log.d(TAG, "YoutubeDL initialized successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize YoutubeDL", e)
            }

            try {
                FFmpeg.getInstance().init(this@VideoGrabApp)
                Log.d(TAG, "FFmpeg initialized successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize FFmpeg", e)
            }

            try {
                Aria2c.getInstance().init(this@VideoGrabApp)
                Log.d(TAG, "Aria2c initialized successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize Aria2c", e)
            }
        }
    }

    companion object {
        private const val TAG = "VideoGrabApp"
    }
}
