package com.socialvideodownloader.shared.feature.history.ui

import java.text.DateFormat
import java.util.Date

actual fun formatTimestamp(epochMillis: Long): String {
    return DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(epochMillis))
}
