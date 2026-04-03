package com.socialvideodownloader.shared.di

import com.socialvideodownloader.shared.feature.download.SharedDownloadViewModel
import com.socialvideodownloader.shared.feature.history.SharedHistoryViewModel
import com.socialvideodownloader.shared.feature.library.SharedLibraryViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.parameter.parametersOf
import org.koin.mp.KoinPlatform
import platform.Foundation.NSUserDefaults

/**
 * Swift-accessible helper for retrieving shared ViewModels from Koin.
 *
 * Each call creates a new ViewModel factory instance with a fresh [CoroutineScope].
 * The Swift caller is responsible for calling [SharedDownloadViewModel.cleanup] when
 * the owning view disappears.
 *
 * Usage from Swift:
 * ```swift
 * let vm = KoinHelper.shared.getDownloadViewModel()
 * ```
 */
object KoinHelper {
    /** Create a new [SharedDownloadViewModel] backed by a managed [CoroutineScope]. */
    fun getDownloadViewModel(): SharedDownloadViewModel {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        return KoinPlatform.getKoin().get { parametersOf(scope) }
    }

    /** Create a new [SharedHistoryViewModel] backed by a managed [CoroutineScope]. */
    fun getHistoryViewModel(): SharedHistoryViewModel {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        return KoinPlatform.getKoin().get { parametersOf(scope) }
    }

    /** Create a new [SharedLibraryViewModel] backed by a managed [CoroutineScope]. */
    fun getLibraryViewModel(): SharedLibraryViewModel {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        return KoinPlatform.getKoin().get { parametersOf(scope) }
    }

    /**
     * Writes a URL to the shared App Group so the Compose DownloadViewModel can pick it up.
     * Called from Swift when an `socialvideodownloader://` URL scheme is opened.
     */
    fun pushSharedUrl(url: String) {
        val defaults = NSUserDefaults(suiteName = APP_GROUP) ?: return
        defaults.setObject(url, forKey = SHARED_URL_KEY)
        defaults.synchronize()
    }

    /**
     * Reads the Share Extension URL from NSUserDefaults if present and re-writes it
     * using the same key that [com.socialvideodownloader.shared.feature.download.platform.PlatformActions]
     * reads, so the Compose DownloadScreen picks it up on the next composition.
     *
     * Called from Swift when the app becomes active.
     */
    fun consumeSharedUrl() {
        val defaults = NSUserDefaults(suiteName = APP_GROUP) ?: return
        // The Share Extension writes to "SharedURL"; PlatformActions reads "pending_shared_url".
        // Normalize by copying to the canonical key if needed.
        val shareExtensionKey = "SharedURL"
        val url = defaults.stringForKey(shareExtensionKey) ?: return
        defaults.removeObjectForKey(shareExtensionKey)
        defaults.setObject(url, forKey = SHARED_URL_KEY)
        defaults.synchronize()
    }
}

private const val SHARED_URL_KEY = "pending_shared_url"
private const val APP_GROUP = "group.com.socialvideodownloader.shared"
