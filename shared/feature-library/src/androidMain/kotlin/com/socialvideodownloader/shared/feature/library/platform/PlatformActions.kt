package com.socialvideodownloader.shared.feature.library.platform

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log

actual class PlatformActions(private val context: Context) {
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
            throw e
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
            throw e
        }
    }
}
