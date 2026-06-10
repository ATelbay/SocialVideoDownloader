package com.socialvideodownloader

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.util.Log
import com.google.firebase.FirebaseApp
import com.socialvideodownloader.core.domain.di.IoDispatcher
import com.socialvideodownloader.core.domain.repository.BillingRepository
import com.socialvideodownloader.core.domain.repository.CloudBackupRepository
import com.socialvideodownloader.core.domain.sync.BackupPreferences
import com.socialvideodownloader.core.domain.sync.CloudAuthService
import com.socialvideodownloader.di.KoinInitializer
import com.socialvideodownloader.feature.download.service.DownloadNotificationManager
import com.yausername.aria2c.Aria2c
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
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

    @Inject
    lateinit var billingRepository: BillingRepository

    @Inject
    lateinit var cloudBackupRepository: CloudBackupRepository

    @Inject
    lateinit var backupPreferences: BackupPreferences

    @Inject
    lateinit var cloudAuthService: CloudAuthService

    private val applicationScope = CoroutineScope(SupervisorJob())

    override fun onCreate() {
        // T063/T137: Initialize Koin BEFORE super.onCreate() completes Hilt setup.
        // Koin must be ready before any Hilt @Provides methods in KoinBridgeModule
        // attempt to retrieve shared dependencies from Koin.
        KoinInitializer.init(this)

        super.onCreate()
        FirebaseApp.initializeApp(this)
        createNotificationChannels()
        initializeLibraries()
        runStartupCloudTasks()
    }

    private fun createNotificationChannels() {
        val channel =
            NotificationChannel(
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

            if (shouldUpdateYoutubeDl()) {
                try {
                    YoutubeDL.getInstance().updateYoutubeDL(
                        this@SocialVideoDownloaderApp,
                        YoutubeDL.UpdateChannel.NIGHTLY,
                    )
                    markYoutubeDlUpdated()
                    Log.d(TAG, "yt-dlp updated successfully")
                } catch (e: Exception) {
                    Log.d(TAG, "yt-dlp update skipped: ${e.message}")
                }
            } else {
                Log.d(TAG, "yt-dlp update throttled (updated within last ${UPDATE_INTERVAL_MS}ms)")
            }
        }
    }

    /**
     * Throttle the yt-dlp self-update so it does not run a network fetch on every cold start.
     * The binary still tracks the NIGHTLY channel for fresh extractor fixes, but at most once
     * per [UPDATE_INTERVAL_MS].
     */
    private fun shouldUpdateYoutubeDl(): Boolean {
        val prefs = getSharedPreferences(UPDATE_PREFS, MODE_PRIVATE)
        val last = prefs.getLong(KEY_LAST_UPDATE, 0L)
        return System.currentTimeMillis() - last >= UPDATE_INTERVAL_MS
    }

    private fun markYoutubeDlUpdated() {
        getSharedPreferences(UPDATE_PREFS, MODE_PRIVATE)
            .edit()
            .putLong(KEY_LAST_UPDATE, System.currentTimeMillis())
            .apply()
    }

    // T064: Restore purchases and update tier limit on startup.
    // T066: Reconcile Firestore counter on startup.
    // Both are fire-and-forget and only run when backup has been enabled at least once.
    private fun runStartupCloudTasks() {
        applicationScope.launch(ioDispatcher) {
            if (!backupPreferences.hasEverEnabled()) return@launch

            // T064: Restore purchases and propagate tier limit to Firestore.
            try {
                val tier = billingRepository.restorePurchases()
                cloudBackupRepository.updateTierLimit(tier.maxRecords)
                Log.d(TAG, "Purchases restored: tier=${tier.name}, limit=${tier.maxRecords}")
            } catch (e: Exception) {
                Log.w(TAG, "Purchase restoration failed on startup: ${e.message}")
            }

            // T066: Reconcile the Firestore counter against actual document count.
            try {
                val uid = cloudAuthService.getCurrentUid() ?: return@launch
                if (uid.isNotEmpty()) {
                    reconcileCounterIfNeeded()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Counter reconciliation failed on startup: ${e.message}")
            }
        }
    }

    private suspend fun reconcileCounterIfNeeded() {
        try {
            val storedCount = cloudBackupRepository.getCloudRecordCount()
            val allRecords = cloudBackupRepository.fetchAllRecords()
            val actualCount = allRecords.size
            if (storedCount != actualCount) {
                Log.d(TAG, "Counter mismatch: stored=$storedCount actual=$actualCount — reconciling")
                cloudBackupRepository.setRecordCount(actualCount)
            }
        } catch (e: Exception) {
            Log.w(TAG, "reconcileCounterIfNeeded failed: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "SocialVideoDownloaderApp"
        private const val UPDATE_PREFS = "ytdlp_update"
        private const val KEY_LAST_UPDATE = "last_update_ms"
        private const val UPDATE_INTERVAL_MS = 24L * 60 * 60 * 1000 // 24h
    }
}
