package com.socialvideodownloader.shared.feature.history.ui

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterNoStyle
import platform.Foundation.NSDateFormatterShortStyle
import platform.Foundation.dateWithTimeIntervalSince1970

actual fun formatTimestamp(epochMillis: Long): String {
    val date = NSDate.dateWithTimeIntervalSince1970(epochMillis / 1000.0)
    val formatter = NSDateFormatter()
    formatter.timeStyle = NSDateFormatterShortStyle
    formatter.dateStyle = NSDateFormatterNoStyle
    return formatter.stringFromDate(date)
}
