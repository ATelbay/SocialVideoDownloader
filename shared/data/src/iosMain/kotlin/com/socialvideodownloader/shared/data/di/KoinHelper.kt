package com.socialvideodownloader.shared.data.di

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
