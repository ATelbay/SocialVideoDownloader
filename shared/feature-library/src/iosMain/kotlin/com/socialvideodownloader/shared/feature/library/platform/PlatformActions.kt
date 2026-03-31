package com.socialvideodownloader.shared.feature.library.platform

import platform.Foundation.NSURL
import platform.UIKit.UIActivity
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication

actual class PlatformActions {
    actual fun openFile(uri: String) {
        val url = NSURL.URLWithString(uri) ?: return
        UIApplication.sharedApplication.openURL(url)
    }

    actual fun shareFile(uri: String) {
        val url = NSURL.URLWithString(uri) ?: return
        val activityVC = UIActivityViewController(
            activityItems = listOf(url),
            applicationActivities = null,
        )
        val rootVC = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return
        rootVC.presentViewController(activityVC, animated = true, completion = null)
    }
}
