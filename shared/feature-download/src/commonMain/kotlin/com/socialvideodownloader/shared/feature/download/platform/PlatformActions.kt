package com.socialvideodownloader.shared.feature.download.platform

expect class PlatformActions {
    fun openFile(uri: String)

    fun shareFile(uri: String)

    fun requestNotificationPermission(): Boolean

    fun getPendingSharedUrl(): String?

    fun clearPendingSharedUrl()
}
