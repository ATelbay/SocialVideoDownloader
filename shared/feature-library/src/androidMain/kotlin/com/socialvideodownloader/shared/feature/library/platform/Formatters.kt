package com.socialvideodownloader.shared.feature.library.platform

import android.text.format.DateUtils
import android.text.format.Formatter
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun formatFileSize(bytes: Long): String {
    val context = LocalContext.current
    return Formatter.formatFileSize(context, bytes)
}

@Composable
actual fun formatRelativeTime(epochMillis: Long): String =
    DateUtils.getRelativeTimeSpanString(
        epochMillis,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
    ).toString()
