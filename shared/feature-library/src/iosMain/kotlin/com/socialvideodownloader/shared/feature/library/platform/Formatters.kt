package com.socialvideodownloader.shared.feature.library.platform

import androidx.compose.runtime.Composable
import platform.Foundation.NSByteCountFormatter
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterMediumStyle
import platform.Foundation.NSDateFormatterNoStyle
import platform.Foundation.dateWithTimeIntervalSince1970

@Composable
actual fun formatFileSize(bytes: Long): String {
    val formatter = NSByteCountFormatter()
    return formatter.stringFromByteCount(bytes)
}

@Composable
actual fun formatRelativeTime(epochMillis: Long): String {
    val date = NSDate.dateWithTimeIntervalSince1970(epochMillis / 1000.0)
    val formatter = NSDateFormatter()
    formatter.dateStyle = NSDateFormatterMediumStyle
    formatter.timeStyle = NSDateFormatterNoStyle
    formatter.doesRelativeDateFormatting = true
    return formatter.stringFromDate(date)
}
