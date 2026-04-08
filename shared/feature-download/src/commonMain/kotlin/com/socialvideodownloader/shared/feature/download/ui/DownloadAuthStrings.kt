package com.socialvideodownloader.shared.feature.download.ui

object DownloadAuthStrings {
    fun authRequiredMessage(platformName: String): String =
        "This content requires authentication. Connect your $platformName account to download."

    fun connectLabel(platformName: String): String = "Connect $platformName"

    fun reconnectLabel(platformName: String): String = "Reconnect $platformName"

    const val disconnectLabel: String = "Disconnect"
}
