package com.socialvideodownloader.shared.feature.library.platform

expect class PlatformActions {
    fun openFile(uri: String)
    fun shareFile(uri: String)
}
