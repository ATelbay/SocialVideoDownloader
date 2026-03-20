package com.socialvideodownloader.core.cloud.auth

import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.socialvideodownloader.core.domain.sync.CloudAuthService
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirebaseCloudAuthService @Inject constructor() : CloudAuthService {

    override suspend fun signInAnonymously(): String {
        val result = Firebase.auth.signInAnonymously().await()
        return result.user?.uid ?: error("Anonymous sign-in succeeded but UID is null")
    }

    override fun getCurrentUid(): String? = Firebase.auth.currentUser?.uid

    override fun isAuthenticated(): Boolean = Firebase.auth.currentUser != null
}
