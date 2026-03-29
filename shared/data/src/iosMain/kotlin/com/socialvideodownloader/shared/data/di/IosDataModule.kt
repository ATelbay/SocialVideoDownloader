package com.socialvideodownloader.shared.data.di

import com.socialvideodownloader.core.domain.repository.VideoExtractorRepository
import com.socialvideodownloader.shared.data.platform.PlatformClipboard
import com.socialvideodownloader.shared.data.platform.PlatformFileStorage
import com.socialvideodownloader.shared.data.platform.PlatformStringProvider
import com.socialvideodownloader.shared.data.repository.ServerOnlyVideoExtractorRepository
import org.koin.dsl.module

/**
 * iOS-specific Koin module providing platform implementations.
 *
 * iOS platform implementations (IosDownloadManager, IosFileStorage, etc.)
 * will be created in Phase 6 (iOS app development). For now, this module
 * provides the ServerOnlyVideoExtractorRepository and placeholder bindings.
 */
val iosDataModule = module {

    // iOS always uses the server API for video extraction — no local yt-dlp.
    single<VideoExtractorRepository> {
        ServerOnlyVideoExtractorRepository(serverApi = get())
    }

    // iOS platform implementations will be added in Phase 6 (T082-T087).
    // Uncomment and replace with actual implementations when building the iOS app:
    // single<PlatformDownloadManager> { IosDownloadManager() }
    // single<PlatformFileStorage> { IosFileStorage() }
    // single<PlatformClipboard> { IosClipboard() }
    // single<PlatformStringProvider> { IosStringProvider() }
}
