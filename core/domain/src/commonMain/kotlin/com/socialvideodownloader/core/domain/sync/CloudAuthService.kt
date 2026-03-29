package com.socialvideodownloader.core.domain.sync

interface CloudAuthService {
    /** Sign in with a Google ID token. Returns UID on success. */
    suspend fun signInWithGoogleCredential(idToken: String): String

    /** Get current UID, or null if not authenticated. */
    fun getCurrentUid(): String?

    /** Check if user is authenticated. */
    fun isAuthenticated(): Boolean

    /** Sign out the current user. */
    suspend fun signOut()

    /** Get display name of the signed-in user, or null. */
    fun getDisplayName(): String?

    /** Get photo URL of the signed-in user, or null. */
    fun getPhotoUrl(): String?
}
