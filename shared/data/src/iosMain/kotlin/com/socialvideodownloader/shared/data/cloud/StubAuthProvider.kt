package com.socialvideodownloader.shared.data.cloud

/**
 * Stub [PlatformAuthProvider] used until the Swift Firebase Auth implementation is wired.
 *
 * Always reports the user as signed out. Sign-in attempts throw an error indicating
 * that the Firebase Auth bridge is not yet configured.
 *
 * Replace by registering the Swift `FirebaseAuthProvider` via `KoinHelper.registerAuthProvider()`
 * in `App.swift` once Firebase is integrated via CocoaPods.
 */
class StubAuthProvider : PlatformAuthProvider {
    override suspend fun signInWithGoogle(idToken: String): String =
        throw UnsupportedOperationException(
            "Firebase Auth not configured — implement FirebaseAuthProvider in Swift and " +
                "register it via KoinHelper.registerAuthProvider() in App.swift",
        )

    override suspend fun signInWithApple(): String =
        throw UnsupportedOperationException(
            "Firebase Auth not configured — implement FirebaseAuthProvider in Swift and " +
                "register it via KoinHelper.registerAuthProvider() in App.swift",
        )

    override suspend fun signOut() {
        // No-op: user is already signed out in stub state
    }

    override fun getCurrentUid(): String? = null

    override fun getDisplayName(): String? = null

    override fun getPhotoUrl(): String? = null
}
