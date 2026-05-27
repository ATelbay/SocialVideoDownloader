package com.socialvideodownloader.shared.feature.history.ui

/**
 * Outcome of a platform Google sign-in attempt.
 *
 * Distinguishes a genuine user cancellation (no error shown) from an actual failure
 * (surfaced to the user) so that sign-in problems are never swallowed silently.
 */
sealed interface GoogleSignInResult {
    data class Success(val idToken: String) : GoogleSignInResult

    data object Cancelled : GoogleSignInResult

    data class Failed(val message: String?) : GoogleSignInResult
}
