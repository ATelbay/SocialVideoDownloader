package com.socialvideodownloader.core.cloud.auth

import com.google.firebase.FirebaseApp
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.socialvideodownloader.core.domain.sync.CloudAuthService
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirebaseCloudAuthService
    @Inject
    constructor() : CloudAuthService {
        private val auth get() = Firebase.auth

        private val isFirebaseInitialized: Boolean
            get() =
                try {
                    FirebaseApp.getInstance()
                    true
                } catch (_: IllegalStateException) {
                    false
                }

        override suspend fun signInWithGoogleCredential(idToken: String): String {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            return result.user?.uid ?: error("Google sign-in succeeded but UID is null")
        }

        override fun getCurrentUid(): String? = if (isFirebaseInitialized) auth.currentUser?.uid else null

        override fun isAuthenticated(): Boolean = isFirebaseInitialized && auth.currentUser != null

        override suspend fun signOut() {
            auth.signOut()
        }

        override fun getDisplayName(): String? = if (isFirebaseInitialized) auth.currentUser?.displayName else null

        override fun getPhotoUrl(): String? = if (isFirebaseInitialized) auth.currentUser?.photoUrl?.toString() else null
    }
