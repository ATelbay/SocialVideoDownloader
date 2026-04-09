package com.socialvideodownloader.shared.feature.library.platform

import platform.Foundation.NSURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentInteractionController

actual class PlatformActions {
    actual fun openFile(uri: String) {
        val url = resolveFileUrl(uri)
        val controller = UIDocumentInteractionController.interactionControllerWithURL(url)
        val rootVC = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return
        controller.presentOptionsMenuFromRect(
            rect = rootVC.view.bounds,
            inView = rootVC.view,
            animated = true,
        )
    }

    actual fun shareFile(uri: String) {
        val url = resolveFileUrl(uri)
        val activityVC =
            UIActivityViewController(
                activityItems = listOf(url),
                applicationActivities = null,
            )
        val rootVC = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return
        rootVC.presentViewController(activityVC, animated = true, completion = null)
    }

    private fun resolveFileUrl(uri: String): NSURL =
        if (uri.startsWith("file://")) {
            NSURL(string = uri)
        } else {
            NSURL(fileURLWithPath = uri)
        }
}
