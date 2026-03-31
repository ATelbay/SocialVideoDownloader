package com.socialvideodownloader.shared.feature.history.platform

import platform.Foundation.NSURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication

actual fun shareFile(uri: String) {
    val url = NSURL.URLWithString(uri) ?: return
    val activityViewController =
        UIActivityViewController(
            activityItems = listOf(url),
            applicationActivities = null,
        )
    val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
    rootViewController?.presentViewController(activityViewController, animated = true, completion = null)
}

/**
 * Google Sign-In via GIDSignIn requires additional cinterop setup.
 * Stub returns failure; the screen handles the sign-in flow via the effect channel.
 */
actual fun triggerGoogleSignIn(): Result<String> = Result.failure(UnsupportedOperationException("GIDSignIn cinterop not yet wired"))

actual fun openUpgradeFlow() {
    // No-op on iOS — App Store upgrade handled externally if needed
}
