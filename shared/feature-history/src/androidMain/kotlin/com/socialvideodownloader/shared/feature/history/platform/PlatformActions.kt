package com.socialvideodownloader.shared.feature.history.platform

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri

private var appContext: Context? = null

fun initPlatformActions(context: Context) {
    appContext = context.applicationContext
}

actual fun shareFile(uri: String) {
    val context = appContext ?: return
    val shareUri = Uri.parse(uri)
    val shareIntent =
        Intent(Intent.ACTION_SEND).apply {
            type = "video/*"
            putExtra(Intent.EXTRA_STREAM, shareUri)
            clipData = ClipData.newRawUri(null, shareUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    context.startActivity(
        Intent.createChooser(shareIntent, null).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        },
    )
}

/**
 * Google Sign-In is handled via Credential Manager in the Android screen composable,
 * which requires an Activity context not available here. This stub returns a failure
 * so callers fall back to the screen-level sign-in flow via [HistoryEffect.LaunchGoogleSignIn].
 */
actual fun triggerGoogleSignIn(): Result<String> =
    Result.failure(UnsupportedOperationException("Use LaunchGoogleSignIn effect on Android"))

actual fun openUpgradeFlow() {
    // Upgrade flow requires Activity context — handled via LaunchUpgradeFlow effect in the screen
}
