package com.socialvideodownloader.core.domain.fake

import com.socialvideodownloader.core.domain.sync.CloudAuthService

class FakeCloudAuthService : CloudAuthService {
    var signedInUid: String? = null
    var signedInIdTokens = mutableListOf<String>()
    var signOutCalled = false

    override suspend fun signInWithGoogleCredential(idToken: String): String {
        signedInIdTokens.add(idToken)
        signedInUid = "uid-$idToken"
        return signedInUid!!
    }

    override fun getCurrentUid(): String? = signedInUid

    override fun isAuthenticated(): Boolean = signedInUid != null

    override suspend fun signOut() {
        signOutCalled = true
        signedInUid = null
    }

    override fun getDisplayName(): String? = null

    override fun getPhotoUrl(): String? = null
}
