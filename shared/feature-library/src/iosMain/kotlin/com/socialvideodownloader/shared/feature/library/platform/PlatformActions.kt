package com.socialvideodownloader.shared.feature.library.platform

import com.socialvideodownloader.shared.data.platform.resolveFileUrl
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentInteractionController
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import platform.UIKit.popoverPresentationController

@OptIn(ExperimentalForeignApi::class)
actual class PlatformActions {
    private var documentController: UIDocumentInteractionController? = null

    actual fun openFile(uri: String) {
        val url = resolveFileUrl(uri)
        val rootVC = rootViewController() ?: return
        val controller = UIDocumentInteractionController.interactionControllerWithURL(url)
        documentController = controller
        controller.presentOptionsMenuFromRect(
            rect = rootVC.view.bounds,
            inView = rootVC.view,
            animated = true,
        )
    }

    actual fun shareFile(uri: String) {
        val url = resolveFileUrl(uri)
        val rootVC = rootViewController() ?: return
        val activityVC =
            UIActivityViewController(
                activityItems = listOf(url),
                applicationActivities = null,
            )
        // Required on iPad: an unanchored popover throws. Anchor it to the centre of the root view.
        activityVC.popoverPresentationController?.let { popover ->
            popover.sourceView = rootVC.view
            rootVC.view.bounds.useContents {
                popover.sourceRect = CGRectMake(size.width / 2.0, size.height / 2.0, 0.0, 0.0)
            }
        }
        rootVC.presentViewController(activityVC, animated = true, completion = null)
    }

    /**
     * Resolves the foreground active window's root view controller via the scene API.
     * Replaces the deprecated [UIApplication.keyWindow], which is unreliable on multi-scene apps.
     */
    private fun rootViewController(): UIViewController? {
        val windowScene =
            UIApplication.sharedApplication.connectedScenes
                .firstOrNull { it is UIWindowScene } as? UIWindowScene ?: return null
        val windows = windowScene.windows
        val keyWindow =
            windows.firstOrNull { (it as? UIWindow)?.isKeyWindow() == true } as? UIWindow
                ?: windows.firstOrNull() as? UIWindow
        return keyWindow?.rootViewController
    }
}
