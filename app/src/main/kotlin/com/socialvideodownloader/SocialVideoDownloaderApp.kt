package com.socialvideodownloader

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.util.Log
import com.yausername.aria2c.Aria2c
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.socialvideodownloader.core.domain.di.IoDispatcher
import com.socialvideodownloader.core.domain.repository.BillingRepository
import com.socialvideodownloader.core.domain.repository.CloudBackupRepository
import com.socialvideodownloader.core.domain.sync.BackupPreferences
import com.socialvideodownloader.core.domain.sync.CloudAuthService
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
        super.onCreate()
        createNotificationChannels()
        initializeLibraries()
        runStartupCloudTasks()
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
    }
}
