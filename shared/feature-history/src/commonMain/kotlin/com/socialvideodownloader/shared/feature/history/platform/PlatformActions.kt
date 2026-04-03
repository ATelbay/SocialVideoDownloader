package com.socialvideodownloader.shared.feature.history.platform

expect fun shareFile(uri: String)

expect fun triggerGoogleSignIn(): Result<String>

expect fun openUpgradeFlow()
