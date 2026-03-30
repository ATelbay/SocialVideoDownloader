package com.socialvideodownloader.shared.feature.download.di

import com.socialvideodownloader.core.domain.usecase.ExtractVideoInfoUseCase
import com.socialvideodownloader.core.domain.usecase.FindExistingDownloadUseCase
import com.socialvideodownloader.shared.data.platform.PlatformDownloadManager
import com.socialvideodownloader.shared.feature.download.SharedDownloadViewModel
import kotlinx.coroutines.CoroutineScope
import org.koin.dsl.module

/**
 * Koin module providing [SharedDownloadViewModel].
 *
 * The VM is a factory (not a singleton) because each screen instance owns its
 * own CoroutineScope and state. On Android the scope comes from
 * [androidx.lifecycle.ViewModel.viewModelScope]; on iOS it is managed by the
 * SwiftUI view.
 *
 * Usage:
 * ```kotlin
 * val vm = getKoin().get<SharedDownloadViewModel> { parametersOf(myScope) }
 * ```
 */
val sharedDownloadModule =
    module {
        factory { (scope: CoroutineScope) ->
            SharedDownloadViewModel(
                coroutineScope = scope,
                extractVideoInfo = get<ExtractVideoInfoUseCase>(),
                findExistingDownload = get<FindExistingDownloadUseCase>(),
                platformDownloadManager = get<PlatformDownloadManager>(),
            )
        }
    }
