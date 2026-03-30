package com.socialvideodownloader.shared.data.cloud

import com.socialvideodownloader.core.domain.sync.CloudAuthService

/**
 * iOS implementation of [CloudAuthService].
 *
 * Delegates all Firebase Auth calls to [PlatformAuthProvider], which is implemented
 * in Swift using the Firebase Auth iOS SDK. This bridge pattern is required because
 * Firebase iOS SDK is Swift-native and cannot be called from Kotlin/Native without
 * a full CocoaPods interop setup.
 *
 * The [PlatformAuthProvider] is registered from Swift at app startup:
 * ```swift
 * // In App.swift or AppDelegate:
 * KoinHelper.shared.registerAuthProvider(FirebaseAuthProvider())
 * ```
 *
 * When the [PlatformAuthProvider] is not yet registered (e.g., in tests), all methods
 * behave as if the user is signed out, and sign-in attempts throw an error.
 */
class IosCloudAuthService(
    private val platformAuthProvider: PlatformAuthProvider,
) : CloudAuthService {
    /**
     * Sign in with a Google ID token. The token is obtained on the Swift side using
     * the Google Sign-In SDK for iOS, then passed here to authenticate with Firebase.
     *
     * Returns the Firebase UID on success.
     */
    override suspend fun signInWithGoogleCredential(idToken: String): String {
        // TODO: Called by Swift after obtaining an ID token via Google Sign-In SDK.
        // Swift flow:
        //   1. GIDSignIn.sharedInstance.signIn(withPresenting: rootViewController)
        //   2. Extract idToken from GIDGoogleUser.idToken.tokenString
        //   3. Call HistoryViewModel.onIntent(HistoryIntent.SignInWithGoogle(idToken))
        //   4. ViewModel calls this method via EnableCloudBackupUseCase
        return platformAuthProvider.signInWithGoogle(idToken)
    }

    override fun getCurrentUid(): String? = platformAuthProvider.getCurrentUid()

    override fun isAuthenticated(): Boolean = platformAuthProvider.getCurrentUid() != null

    override suspend fun signOut() {
        platformAuthProvider.signOut()
    }

    override fun getDisplayName(): String? = platformAuthProvider.getDisplayName()

    override fun getPhotoUrl(): String? = platformAuthProvider.getPhotoUrl()
}
