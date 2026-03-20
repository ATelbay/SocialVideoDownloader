package com.socialvideodownloader.core.domain.sync

interface CloudAuthService {
    /** Sign in anonymously. Returns UID on success. */
    suspend fun signInAnonymously(): String

    /** Get current UID, or null if not authenticated. */
    fun getCurrentUid(): String?

    /** Check if user is authenticated. */
    fun isAuthenticated(): Boolean
}
