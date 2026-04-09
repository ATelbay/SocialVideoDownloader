package com.socialvideodownloader.shared.feature.library.platform

import com.socialvideodownloader.shared.data.platform.resolveFileUrl
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentInteractionController

@OptIn(ExperimentalForeignApi::class)
actual class PlatformActions {
    private var documentController: UIDocumentInteractionController? = null

    actual fun openFile(uri: String) {
        val url = resolveFileUrl(uri)
        documentController = UIDocumentInteractionController.interactionControllerWithURL(url)
        val rootVC = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return
        documentController!!.presentOptionsMenuFromRect(
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
}
