package com.socialvideodownloader.shared.data.cloud

/**
 * Bridge interface implemented by Swift to provide Firebase Auth and Sign in with Apple
 * capabilities to the Kotlin layer.
 *
 * Firebase Auth iOS SDK is Swift-native and cannot be called directly from Kotlin/Native
 * without CocoaPods interop setup. This interface decouples the Kotlin implementation from
 * the Swift-side SDK calls.
 *
 * Swift implementation registered at app startup via KoinHelper.registerAuthProvider().
 */
interface PlatformAuthProvider {
    /**
     * Sign in using a Google ID token obtained from the Google Sign-In SDK on iOS.
     * Returns the Firebase UID on success.
     * Throws an exception with a user-readable message on failure.
     */
    suspend fun signInWithGoogle(idToken: String): String

    /**
     * Sign in using Sign in with Apple.
     * Returns the Firebase UID on success.
     * Throws an exception with a user-readable message on failure.
     */
    suspend fun signInWithApple(): String

    /**
     * Sign out the currently authenticated Firebase user.
     */
    suspend fun signOut()

    /** Returns the UID of the currently authenticated user, or null. */
    fun getCurrentUid(): String?

    /** Returns the display name of the signed-in user, or null. */
    fun getDisplayName(): String?

    /** Returns the photo URL of the signed-in user, or null. */
    fun getPhotoUrl(): String?
}
