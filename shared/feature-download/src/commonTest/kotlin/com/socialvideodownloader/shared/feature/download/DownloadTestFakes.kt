package com.socialvideodownloader.shared.feature.download

import com.socialvideodownloader.core.domain.file.FileAccessManager
import com.socialvideodownloader.core.domain.model.DownloadRecord
import com.socialvideodownloader.core.domain.model.DownloadRequest
import com.socialvideodownloader.core.domain.model.VideoFormatOption
import com.socialvideodownloader.core.domain.model.VideoMetadata
import com.socialvideodownloader.core.domain.repository.DownloadRepository
import com.socialvideodownloader.core.domain.repository.VideoExtractorRepository
import com.socialvideodownloader.core.domain.usecase.ExtractVideoInfoUseCase
import com.socialvideodownloader.core.domain.usecase.FindExistingDownloadUseCase
import com.socialvideodownloader.shared.data.platform.DownloadServiceState
import com.socialvideodownloader.shared.data.platform.PlatformDownloadManager
import com.socialvideodownloader.shared.network.auth.CookieStore
import com.socialvideodownloader.shared.network.auth.SupportedPlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope

// ---------------------------------------------------------------------------
// Configurable fakes shared by the SharedDownloadViewModel test suite.
// ---------------------------------------------------------------------------

/**
 * Extractor whose behaviour is driven by [onExtract], which receives the URL and
 * the zero-based call index so tests can simulate "fail then succeed" retry flows.
 */
internal class FakeVideoExtractorRepository(
    private val onExtract: suspend (url: String, callIndex: Int) -> VideoMetadata,
) : VideoExtractorRepository {
    var extractCount: Int = 0
        private set

    override suspend fun extractInfo(url: String): VideoMetadata {
        val index = extractCount
        extractCount++
        return onExtract(url, index)
    }

    override suspend fun download(
        request: DownloadRequest,
        callback: (Float, Long, String) -> Unit,
    ): String = throw UnsupportedOperationException()

    override fun cancelDownload(processId: String) = Unit

    companion object {
        /** Always throws [message]. */
        fun alwaysFails(message: String) = FakeVideoExtractorRepository { _, _ -> throw Exception(message) }

        /** Always returns [metadata]. */
        fun alwaysSucceeds(metadata: VideoMetadata) = FakeVideoExtractorRepository { _, _ -> metadata }
    }
}

internal class FakeDownloadRepository(
    private val completed: List<DownloadRecord> = emptyList(),
) : DownloadRepository {
    override fun getAll(): Flow<List<DownloadRecord>> = flowOf(emptyList())

    override fun getCompletedDownloads(): Flow<List<DownloadRecord>> = flowOf(emptyList())

    override suspend fun getById(id: Long): DownloadRecord? = null

    override suspend fun getCompletedSnapshot(): List<DownloadRecord> = completed

    override suspend fun insert(record: DownloadRecord): Long = 0L

    override suspend fun update(record: DownloadRecord) = Unit

    override suspend fun delete(record: DownloadRecord) = Unit

    override suspend fun deleteAll() = Unit
}

internal class FakeFileAccessManager : FileAccessManager {
    override suspend fun resolveContentUri(record: DownloadRecord): String? = null

    override suspend fun isFileAccessible(contentUri: String): Boolean = false

    override suspend fun deleteFile(contentUri: String): Boolean = false
}

/** Download manager whose [downloadState] can be driven from tests via [emit]. */
internal class ControllablePlatformDownloadManager : PlatformDownloadManager {
    private val state = MutableStateFlow<DownloadServiceState>(DownloadServiceState.Idle)
    override val downloadState: StateFlow<DownloadServiceState> = state

    val startedRequests = mutableListOf<DownloadRequest>()
    val cancelledIds = mutableListOf<String>()

    override val activeRequestId: String?
        get() = startedRequests.lastOrNull()?.id

    override suspend fun startDownload(request: DownloadRequest) {
        startedRequests += request
    }

    override fun cancelDownload(requestId: String) {
        cancelledIds += requestId
    }

    fun emit(serviceState: DownloadServiceState) {
        state.value = serviceState
    }
}

internal class FakeCookieStore(
    connected: Set<SupportedPlatform> = emptySet(),
) : CookieStore {
    private val store = mutableMapOf<SupportedPlatform, String>()

    init {
        connected.forEach { store[it] = "cookie" }
    }

    override fun getCookies(platform: SupportedPlatform): String? = store[platform]

    override fun setCookies(
        platform: SupportedPlatform,
        cookies: String,
    ) {
        store[platform] = cookies
    }

    override fun clearCookies(platform: SupportedPlatform) {
        store.remove(platform)
    }

    override fun isConnected(platform: SupportedPlatform): Boolean = store.containsKey(platform)

    override fun connectedPlatforms(): List<SupportedPlatform> = store.keys.toList()
}

// ---------------------------------------------------------------------------
// Builders
// ---------------------------------------------------------------------------

internal fun videoFormat(
    formatId: String,
    isAudioOnly: Boolean = false,
    resolution: Int? = if (isAudioOnly) null else 720,
    ext: String = if (isAudioOnly) "m4a" else "mp4",
    fileSizeBytes: Long? = 1_000_000L,
): VideoFormatOption =
    VideoFormatOption(
        formatId = formatId,
        label = formatId,
        resolution = resolution,
        ext = ext,
        fileSizeBytes = fileSizeBytes,
        isAudioOnly = isAudioOnly,
        isVideoOnly = false,
    )

internal fun videoMetadata(
    sourceUrl: String = "https://www.youtube.com/watch?v=abc",
    title: String = "Test Video",
    formats: List<VideoFormatOption> = listOf(videoFormat("v720")),
): VideoMetadata =
    VideoMetadata(
        sourceUrl = sourceUrl,
        title = title,
        thumbnailUrl = null,
        durationSeconds = 100,
        author = null,
        formats = formats,
    )

/**
 * Creates a VM backed by a scope tied to the TestScheduler so advanceUntilIdle()
 * advances its coroutines, but using a child Job so vm.cleanup() cancels it
 * without cancelling the entire test scope.
 */
internal fun makeVm(
    scope: TestScope,
    extractor: FakeVideoExtractorRepository,
    downloadManager: ControllablePlatformDownloadManager = ControllablePlatformDownloadManager(),
    cookieStore: CookieStore = FakeCookieStore(),
    downloadRepository: DownloadRepository = FakeDownloadRepository(),
): SharedDownloadViewModel {
    val extractUseCase = ExtractVideoInfoUseCase(extractor)
    val finder = FindExistingDownloadUseCase(downloadRepository, FakeFileAccessManager())
    val vmScope =
        CoroutineScope(scope.coroutineContext + SupervisorJob(scope.coroutineContext[Job]))
    return SharedDownloadViewModel(
        coroutineScope = vmScope,
        extractVideoInfo = extractUseCase,
        findExistingDownload = finder,
        platformDownloadManager = downloadManager,
        secureCookieStore = cookieStore,
    )
}
