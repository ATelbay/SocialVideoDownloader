package com.socialvideodownloader.shared.feature.download

import com.socialvideodownloader.shared.data.platform.DownloadErrorType
import com.socialvideodownloader.shared.network.ServerExtractionException
import com.socialvideodownloader.shared.network.auth.detectPlatform
import com.socialvideodownloader.shared.network.auth.detectPlatformFromError

/**
 * Maps a raw extraction [Throwable] to a user-facing [DownloadErrorType].
 *
 * Extracted from [SharedDownloadViewModel] so the keyword heuristics (which are
 * inherently fragile against yt-dlp message changes) are unit-testable in
 * isolation as a pure function. No state, no side effects.
 *
 * Public so platform layers (e.g. the Android download service) classify failures
 * with the same heuristics instead of maintaining a divergent copy.
 */
object DownloadErrorClassifier {
    /** Strong auth signals: combined with a supported platform → AUTH_REQUIRED. */
    private val authKeywords =
        listOf("sign in", "login required", "must be logged in", "inappropriate", "age-restricted", "age restricted", "nsfw")

    /**
     * Weaker auth-adjacent signals. Only treated as AUTH_REQUIRED when the URL's
     * platform also matches the platform tagged in the error (e.g. `[youtube]`),
     * so generic "Video unavailable" errors don't wrongly offer a login.
     */
    private val authFallbackKeywords =
        listOf("login", "sign in", "private", "restricted", "members only", "subscriber", "authenticate", "credentials")

    fun classify(
        error: Throwable,
        url: String,
    ): DownloadErrorType {
        val message = error.message ?: return DownloadErrorType.UNKNOWN
        val lower = message.lowercase()

        // Check auth-required first, before the ServerExtractionException short-circuit,
        // because the WS proxy wraps yt-dlp auth errors as ServerExtractionException.
        if (authKeywords.any { lower.contains(it) } && detectPlatform(url) != null) {
            return DownloadErrorType.AUTH_REQUIRED
        }

        val hasAuthHint = authFallbackKeywords.any { it in lower }
        val platformFromUrl = detectPlatform(url)
        val platformFromError = detectPlatformFromError(message)
        if (platformFromUrl != null && platformFromUrl == platformFromError && hasAuthHint) {
            return DownloadErrorType.AUTH_REQUIRED
        }

        if (error is ServerExtractionException) {
            return DownloadErrorType.EXTRACTION_FAILED
        }

        return when {
            message.contains("Unsupported URL", ignoreCase = true) -> DownloadErrorType.UNSUPPORTED_URL
            // Server unreachable / backend down (check before generic "unavailable").
            message.contains("Connection refused", ignoreCase = true) ||
                message.contains("ECONNREFUSED", ignoreCase = true) ||
                message.contains("server error", ignoreCase = true) ||
                message.contains("502", ignoreCase = true) ||
                message.contains("503", ignoreCase = true) ||
                message.contains("504", ignoreCase = true) -> DownloadErrorType.SERVER_UNAVAILABLE
            // Copyright takedowns get a dedicated, actionable message; check before the
            // generic unavailable/private branch since yt-dlp often pairs those words.
            message.contains("copyright", ignoreCase = true) -> DownloadErrorType.COPYRIGHT
            message.contains("unavailable", ignoreCase = true) ||
                message.contains("private", ignoreCase = true) -> DownloadErrorType.EXTRACTION_FAILED
            message.contains("network", ignoreCase = true) ||
                message.contains("connect", ignoreCase = true) ||
                message.contains("timed out", ignoreCase = true) ||
                message.contains("timeout", ignoreCase = true) ||
                message.contains("internet", ignoreCase = true) -> DownloadErrorType.NETWORK_ERROR
            message.contains("space", ignoreCase = true) ||
                message.contains("storage", ignoreCase = true) -> DownloadErrorType.STORAGE_FULL
            else -> DownloadErrorType.UNKNOWN
        }
    }
}
