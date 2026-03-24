package com.socialvideodownloader.core.ui.util

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log

fun Context.shareVideo(contentUri: String) {
    try {
        val shareUri = Uri.parse(contentUri)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "video/*"
            putExtra(Intent.EXTRA_STREAM, shareUri)
            clipData = ClipData.newRawUri(null, shareUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(
            Intent.createChooser(shareIntent, null).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            },
        )
    } catch (e: Exception) {
        Log.e("IntentUtils", "Failed to share video", e)
        throw e
    }
}

fun Context.openVideo(contentUri: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.parse(contentUri), "video/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    } catch (e: Exception) {
        Log.e("IntentUtils", "Failed to open video", e)
        throw e
    }
}
