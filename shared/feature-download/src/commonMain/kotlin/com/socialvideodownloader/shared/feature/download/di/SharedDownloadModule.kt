package com.socialvideodownloader.shared.feature.download.di

import com.socialvideodownloader.core.domain.usecase.ExtractVideoInfoUseCase
import com.socialvideodownloader.core.domain.usecase.FindExistingDownloadUseCase
import com.socialvideodownloader.shared.data.platform.PlatformDownloadManager
import com.socialvideodownloader.shared.feature.download.SharedDownloadViewModel
import com.socialvideodownloader.shared.network.auth.CookieStore
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
 * IMPORTANT: the supplied [CoroutineScope] MUST be main-confined (single-threaded /
 * Main.immediate). [SharedDownloadViewModel] mutates plain fields without
 * synchronization and relies on that confinement — do not pass a multi-threaded
 * dispatcher here. Both production call sites (viewModelScope, the iOS main scope)
 * satisfy this.
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
                secureCookieStore = get<CookieStore>(),
            )
        }
    }
