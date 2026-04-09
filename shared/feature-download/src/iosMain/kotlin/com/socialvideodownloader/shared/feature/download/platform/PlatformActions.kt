package com.socialvideodownloader.shared.feature.download.platform

import com.socialvideodownloader.shared.data.platform.resolveFileUrl
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSUserDefaults
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentInteractionController
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNUserNotificationCenter

private const val SHARED_URL_KEY = "pending_shared_url"
private const val APP_GROUP = "group.com.socialvideodownloader.shared"

@OptIn(ExperimentalForeignApi::class)
actual class PlatformActions {
    private var documentController: UIDocumentInteractionController? = null

    actual fun openFile(uri: String) {
        val url = resolveFileUrl(uri)
        documentController = UIDocumentInteractionController.interactionControllerWithURL(url)
        val rootVc = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return
        documentController!!.presentOptionsMenuFromRect(
            rect = rootVc.view.bounds,
            inView = rootVc.view,
            animated = true,
        )
    }

    actual fun shareFile(uri: String) {
        val url = resolveFileUrl(uri)
        val activityVc =
            UIActivityViewController(
                activityItems = listOf(url),
                applicationActivities = null,
            )
        val rootVc = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return
        rootVc.presentViewController(activityVc, animated = true, completion = null)
    }

    actual fun requestNotificationPermission(): Boolean {
        var granted = false
        UNUserNotificationCenter.currentNotificationCenter().requestAuthorizationWithOptions(
            options = UNAuthorizationOptionAlert or UNAuthorizationOptionBadge or UNAuthorizationOptionSound,
        ) { result, _ ->
            granted = result
        }
        return granted
    }

    actual fun getPendingSharedUrl(): String? {
        val defaults = NSUserDefaults(suiteName = APP_GROUP) ?: return null
        return defaults.stringForKey(SHARED_URL_KEY)
    }

    actual fun clearPendingSharedUrl() {
        val defaults = NSUserDefaults(suiteName = APP_GROUP) ?: return
        defaults.removeObjectForKey(SHARED_URL_KEY)
    }
}
