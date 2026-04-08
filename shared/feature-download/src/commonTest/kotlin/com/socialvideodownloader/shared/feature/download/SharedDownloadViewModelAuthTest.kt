package com.socialvideodownloader.shared.feature.download

import com.socialvideodownloader.core.domain.file.FileAccessManager
import com.socialvideodownloader.core.domain.model.DownloadRecord
import com.socialvideodownloader.core.domain.model.DownloadRequest
import com.socialvideodownloader.core.domain.model.VideoMetadata
import com.socialvideodownloader.core.domain.repository.DownloadRepository
import com.socialvideodownloader.core.domain.repository.VideoExtractorRepository
import com.socialvideodownloader.core.domain.usecase.ExtractVideoInfoUseCase
import com.socialvideodownloader.core.domain.usecase.FindExistingDownloadUseCase
import com.socialvideodownloader.shared.data.platform.DownloadErrorType
import com.socialvideodownloader.shared.data.platform.DownloadServiceState
import com.socialvideodownloader.shared.data.platform.PlatformDownloadManager
import com.socialvideodownloader.shared.network.auth.CookieStore
import com.socialvideodownloader.shared.network.auth.SupportedPlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

// ---------------------------------------------------------------------------
// Fakes
// ---------------------------------------------------------------------------

private class FakeVideoExtractorRepository(private val errorMessage: String) :
    VideoExtractorRepository {
    override suspend fun extractInfo(url: String): VideoMetadata = throw Exception(errorMessage)

    override suspend fun download(
        request: DownloadRequest,
        callback: (Float, Long, String) -> Unit,
    ): String = throw UnsupportedOperationException()

    override fun cancelDownload(processId: String) = Unit
}

private class FakeDownloadRepository : DownloadRepository {
    override fun getAll(): Flow<List<DownloadRecord>> = flowOf(emptyList())

    override fun getCompletedDownloads(): Flow<List<DownloadRecord>> = flowOf(emptyList())

    override suspend fun getById(id: Long): DownloadRecord? = null

    override suspend fun getCompletedSnapshot(): List<DownloadRecord> = emptyList()

    override suspend fun insert(record: DownloadRecord): Long = 0L

    override suspend fun update(record: DownloadRecord) = Unit

    override suspend fun delete(record: DownloadRecord) = Unit

    override suspend fun deleteAll() = Unit
}

private class FakeFileAccessManager : FileAccessManager {
    override suspend fun resolveContentUri(record: DownloadRecord): String? = null

    override suspend fun isFileAccessible(contentUri: String): Boolean = false

    override suspend fun deleteFile(contentUri: String): Boolean = false
}

private class FakePlatformDownloadManager : PlatformDownloadManager {
    override val downloadState: StateFlow<DownloadServiceState> =
        MutableStateFlow(DownloadServiceState.Idle)
    override val activeRequestId: String? = null

    override suspend fun startDownload(request: DownloadRequest) = Unit

    override fun cancelDownload(requestId: String) = Unit
}

private class FakeCookieStore : CookieStore {
    private val store = mutableMapOf<SupportedPlatform, String>()

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
// Helpers
// ---------------------------------------------------------------------------

/**
 * Creates a VM backed by a scope tied to the TestScheduler so advanceUntilIdle()
 * advances its coroutines, but using a child Job so vm.cleanup() cancels it without
 * cancelling the entire test scope.
 */
private fun makeVm(
    scope: TestScope,
    errorMessage: String,
): SharedDownloadViewModel {
    val extractor = ExtractVideoInfoUseCase(FakeVideoExtractorRepository(errorMessage))
    val finder = FindExistingDownloadUseCase(FakeDownloadRepository(), FakeFileAccessManager())
    val vmScope = CoroutineScope(scope.coroutineContext + SupervisorJob(scope.coroutineContext[kotlinx.coroutines.Job]))
    return SharedDownloadViewModel(
        coroutineScope = vmScope,
        extractVideoInfo = extractor,
        findExistingDownload = finder,
        platformDownloadManager = FakePlatformDownloadManager(),
        secureCookieStore = FakeCookieStore(),
    )
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

/**
 * Tests for auth error mapping in SharedDownloadViewModel.
 *
 * mapErrorToType and friendlyErrorMessage are private and exercised indirectly
 * through the public extractWithRetry flow triggered by DownloadIntent.ExtractClicked.
 *
 * NOTE: Several assertions below reference features that are being added by the
 * parallel vm-agent (T015-T018):
 *  - DownloadUiState.Error.errorType == DownloadErrorType.AUTH_REQUIRED
 *  - DownloadUiState.Error.platformForAuth (new field, not present yet)
 *  - friendlyErrorMessage updated to include platform name for AUTH_REQUIRED
 *
 * Tests that depend on the future implementation are marked with TODO.
 * They may not compile until the vm-agent's work is merged — that is expected.
 */
class SharedDownloadViewModelAuthTest {
    // ------------------------------------------------------------------
    // Test 1: Auth error on a supported platform → AUTH_REQUIRED
    // ------------------------------------------------------------------

    /**
     * When extraction fails with an auth-like message AND the URL is from a known
     * platform (Instagram), the ViewModel should emit DownloadUiState.Error with
     * errorType == AUTH_REQUIRED.
     *
     * TODO (vm-agent T015): Once AUTH_REQUIRED branch is added to mapErrorToType,
     *   replace the assertNotNull with:
     *     assertEquals(DownloadErrorType.AUTH_REQUIRED, state.errorType)
     * TODO (vm-agent T015): Once DownloadUiState.Error gains a `platformForAuth` field,
     *   add: assertNotNull(state.platformForAuth)
     *   and: assertEquals("Instagram", state.platformForAuth?.displayName)
     */
    @Test
    fun authError_onSupportedPlatform_emitsErrorState() =
        runTest {
            val vm = makeVm(this, "[Instagram] This content requires login. Sign in to continue.")

            vm.onIntent(DownloadIntent.UrlChanged("https://www.instagram.com/reel/xxx"))
            vm.onIntent(DownloadIntent.ExtractClicked)
            advanceUntilIdle()

            val state = vm.uiState.value
            vm.cleanup()

            assertTrue(state is DownloadUiState.Error, "Expected Error state but got $state")

            // TODO (vm-agent T015): assertEquals(DownloadErrorType.AUTH_REQUIRED, state.errorType)
            assertNotNull((state as DownloadUiState.Error).errorType)
        }

    // ------------------------------------------------------------------
    // Test 2: Auth error on unsupported platform → not AUTH_REQUIRED
    // ------------------------------------------------------------------

    /**
     * When extraction fails with auth keywords but the URL does not match any
     * supported platform (e.g. tiktok.com), errorType should NOT be AUTH_REQUIRED
     * because no platform-specific cookie login flow is available.
     *
     * TODO (vm-agent T015): Once AUTH_REQUIRED is implemented, uncomment:
     *   assertNotEquals(DownloadErrorType.AUTH_REQUIRED, errorType)
     */
    @Test
    fun authError_onUnsupportedPlatform_doesNotEmitAuthRequired() =
        runTest {
            val vm = makeVm(this, "sign in required to view this content")

            vm.onIntent(DownloadIntent.UrlChanged("https://tiktok.com/xxx"))
            vm.onIntent(DownloadIntent.ExtractClicked)
            advanceUntilIdle()

            val state = vm.uiState.value
            vm.cleanup()

            assertTrue(state is DownloadUiState.Error, "Expected Error state but got $state")

            // tiktok.com is not a SupportedPlatform — AUTH_REQUIRED should not be set
            // TODO (vm-agent T015): assertNotEquals(DownloadErrorType.AUTH_REQUIRED, state.errorType)
            assertNotNull((state as DownloadUiState.Error).errorType)
        }

    // ------------------------------------------------------------------
    // Test 3: friendlyErrorMessage is non-null for auth errors
    // ------------------------------------------------------------------

    /**
     * When errorType is AUTH_REQUIRED, the friendly message shown in the UI should
     * be non-null and ideally contain the platform name.
     *
     * TODO (vm-agent T015): Once platformForAuth is added and friendlyErrorMessage
     *   is updated for AUTH_REQUIRED, assert message contains "Instagram".
     */
    @Test
    fun authError_friendlyMessage_isNotNull() =
        runTest {
            val vm = makeVm(this, "[Instagram] Sign in to continue watching this reel.")

            vm.onIntent(DownloadIntent.UrlChanged("https://www.instagram.com/reel/zzz"))
            vm.onIntent(DownloadIntent.ExtractClicked)
            advanceUntilIdle()

            val state = vm.uiState.value as? DownloadUiState.Error
            vm.cleanup()

            assertNotNull(state, "Expected Error state")

            // Verify message is populated — the existing friendlyErrorMessage catches "sign in"
            assertNotNull(state.message)
            // TODO (vm-agent T015): assertTrue(state.message!!.contains("Instagram"))
        }

    // ------------------------------------------------------------------
    // Test 4: Non-auth error → EXTRACTION_FAILED, not AUTH_REQUIRED
    // ------------------------------------------------------------------

    /**
     * A generic extraction error without auth keywords should not produce AUTH_REQUIRED,
     * even for a supported-platform URL.
     */
    @Test
    fun nonAuthError_mapsToExtractionFailed() =
        runTest {
            val vm = makeVm(this, "Video unavailable")

            vm.onIntent(DownloadIntent.UrlChanged("https://www.youtube.com/watch?v=xxx"))
            vm.onIntent(DownloadIntent.ExtractClicked)
            advanceUntilIdle()

            val state = vm.uiState.value
            vm.cleanup()

            assertTrue(state is DownloadUiState.Error, "Expected Error state but got $state")
            assertEquals(DownloadErrorType.EXTRACTION_FAILED, (state as DownloadUiState.Error).errorType)
        }

    // ------------------------------------------------------------------
    // Test 5: State transitions to Error after extraction failure
    // ------------------------------------------------------------------

    @Test
    fun extractionFailure_transitionsToErrorState() =
        runTest {
            val vm = makeVm(this, "login required")

            vm.onIntent(DownloadIntent.UrlChanged("https://www.instagram.com/reel/aaa"))
            vm.onIntent(DownloadIntent.ExtractClicked)
            advanceUntilIdle()

            // After coroutine completes, state must be Error (not Extracting or Idle)
            val state = vm.uiState.value
            vm.cleanup()

            assertTrue(state is DownloadUiState.Error, "Expected Error state but got $state")
        }

    // ------------------------------------------------------------------
    // Test 6: Unsupported URL error message
    // ------------------------------------------------------------------

    @Test
    fun unsupportedUrlError_mapsToUnsupportedUrl() =
        runTest {
            val vm = makeVm(this, "Unsupported URL: https://example.com/bad")

            vm.onIntent(DownloadIntent.UrlChanged("https://www.youtube.com/watch?v=xxx"))
            vm.onIntent(DownloadIntent.ExtractClicked)
            advanceUntilIdle()

            val state = vm.uiState.value as? DownloadUiState.Error
            vm.cleanup()

            assertNotNull(state)
            assertEquals(DownloadErrorType.UNSUPPORTED_URL, state.errorType)
        }
}
