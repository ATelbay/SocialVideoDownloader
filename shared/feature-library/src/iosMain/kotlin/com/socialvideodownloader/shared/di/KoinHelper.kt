package com.socialvideodownloader.shared.di

import com.socialvideodownloader.shared.feature.download.SharedDownloadViewModel
import com.socialvideodownloader.shared.feature.history.SharedHistoryViewModel
import com.socialvideodownloader.shared.feature.library.SharedLibraryViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.parameter.parametersOf
import org.koin.mp.KoinPlatform

/**
 * Swift-accessible helper for retrieving shared ViewModels from Koin.
 *
 * Each call creates a new ViewModel factory instance with a fresh [CoroutineScope].
 * The Swift caller is responsible for calling [SharedDownloadViewModel.cleanup] when
 * the owning SwiftUI view disappears.
 *
 * Usage from Swift:
 * ```swift
 * let vm = KoinHelper.shared.getDownloadViewModel()
 * ```
 *
 * TODO: Tech debt — this file lives in :shared:feature-library but depends on all three
 *   feature modules (feature-download, feature-history, feature-library). It should be
 *   moved to a dedicated :shared:di or :shared:app-entry module so that feature-library
 *   does not transitively depend on feature-download and feature-history on iOS.
 *   Requires updating build.gradle.kts iOS dependencies and the iOS app's framework import.
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
}
