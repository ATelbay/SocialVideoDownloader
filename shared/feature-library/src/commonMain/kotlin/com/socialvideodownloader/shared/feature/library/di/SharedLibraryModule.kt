package com.socialvideodownloader.shared.feature.library.di

import com.socialvideodownloader.core.domain.file.FileAccessManager
import com.socialvideodownloader.core.domain.repository.DownloadRepository
import com.socialvideodownloader.shared.feature.library.SharedLibraryViewModel
import kotlinx.coroutines.CoroutineScope
import org.koin.dsl.module

/**
 * Koin module providing [SharedLibraryViewModel].
 *
 * The VM is a factory — each screen instance owns its own CoroutineScope.
 *
 * Usage:
 * ```kotlin
 * val vm = getKoin().get<SharedLibraryViewModel> { parametersOf(myScope) }
 * ```
 */
val sharedLibraryModule = module {
    factory { (scope: CoroutineScope) ->
        SharedLibraryViewModel(
            coroutineScope = scope,
            downloadRepository = get<DownloadRepository>(),
            fileManager = get<FileAccessManager>(),
        )
    }
}
