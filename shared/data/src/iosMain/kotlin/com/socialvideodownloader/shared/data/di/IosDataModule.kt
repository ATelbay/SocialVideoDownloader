package com.socialvideodownloader.shared.data.di

import com.socialvideodownloader.core.domain.repository.VideoExtractorRepository
import com.socialvideodownloader.shared.data.platform.IosClipboard
import com.socialvideodownloader.shared.data.platform.IosDownloadManager
import com.socialvideodownloader.shared.data.platform.IosFileStorage
import com.socialvideodownloader.shared.data.platform.IosStringProvider
import com.socialvideodownloader.shared.data.platform.PlatformClipboard
import com.socialvideodownloader.shared.data.platform.PlatformDownloadManager
import com.socialvideodownloader.shared.data.platform.PlatformFileStorage
import com.socialvideodownloader.shared.data.platform.PlatformStringProvider
import com.socialvideodownloader.shared.data.repository.ServerOnlyVideoExtractorRepository
import org.koin.dsl.module

/**
 * iOS-specific Koin module providing platform implementations.
 *
 * Binds all platform abstractions to their iOS implementations wired up in Phase 6.
 */
val iosDataModule = module {

    // iOS always uses the server API for video extraction — no local yt-dlp.
    single<VideoExtractorRepository> {
        ServerOnlyVideoExtractorRepository(serverApi = get())
    }

    // Phase 6 platform implementations.
    single<PlatformDownloadManager> { IosDownloadManager() }
    single<PlatformFileStorage> { IosFileStorage() }
    single<PlatformClipboard> { IosClipboard() }
    single<PlatformStringProvider> { IosStringProvider() }
}
