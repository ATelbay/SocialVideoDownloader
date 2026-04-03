package com.socialvideodownloader.shared.feature.download.platform

import android.Manifest
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat

actual class PlatformActions(
    private val context: Context,
    private val onRequestPermission: (() -> Unit)? = null,
) {
    actual fun openFile(uri: String) {
        try {
            val intent =
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(Uri.parse(uri), "video/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("PlatformActions", "Failed to open file", e)
        }
    }

    actual fun shareFile(uri: String) {
        try {
            val shareUri = Uri.parse(uri)
            val shareIntent =
                Intent(Intent.ACTION_SEND).apply {
                    type = "video/*"
                    putExtra(Intent.EXTRA_STREAM, shareUri)
                    clipData = ClipData.newRawUri(null, shareUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            context.startActivity(
                Intent.createChooser(shareIntent, null).apply {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                },
            )
        } catch (e: Exception) {
            Log.e("PlatformActions", "Failed to share file", e)
        }
    }

    actual fun requestNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        val granted =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            onRequestPermission?.invoke()
        }
        return granted
    }

    actual fun getPendingSharedUrl(): String? = null

    actual fun clearPendingSharedUrl() = Unit
}
