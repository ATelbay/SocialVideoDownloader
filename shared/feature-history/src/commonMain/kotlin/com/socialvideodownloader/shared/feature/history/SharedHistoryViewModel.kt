package com.socialvideodownloader.shared.feature.history

import com.socialvideodownloader.core.domain.file.FileAccessManager
import com.socialvideodownloader.core.domain.model.DownloadStatus
import com.socialvideodownloader.core.domain.model.HistoryItem
import com.socialvideodownloader.core.domain.repository.BillingRepository
import com.socialvideodownloader.core.domain.repository.DownloadRepository
import com.socialvideodownloader.core.domain.sync.BackupPreferences
import com.socialvideodownloader.core.domain.sync.CloudAuthService
import com.socialvideodownloader.core.domain.sync.DisableCloudBackupUseCase
import com.socialvideodownloader.core.domain.sync.EnableCloudBackupUseCase
import com.socialvideodownloader.core.domain.sync.ObserveCloudCapacityUseCase
import com.socialvideodownloader.core.domain.sync.RestoreFromCloudUseCase
import com.socialvideodownloader.core.domain.sync.SyncManager
import com.socialvideodownloader.shared.data.platform.PlatformClipboard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Platform-independent shared ViewModel for the history screen.
 *
 * Contains all state machine logic extracted from the Android HistoryViewModel.
 * Uses [PlatformClipboard] for clipboard operations instead of Android ClipboardManager.
 */
class SharedHistoryViewModel(
    private val coroutineScope: CoroutineScope,
    private val downloadRepository: DownloadRepository,
    private val fileManager: FileAccessManager,
    private val deleteHistoryItemUseCase: DeleteHistoryItemUseCaseShared,
    private val observeCloudCapacity: ObserveCloudCapacityUseCase,
    private val billingRepository: BillingRepository,
    private val enableCloudBackupUseCase: EnableCloudBackupUseCase,
    private val disableCloudBackupUseCase: DisableCloudBackupUseCase,
    private val syncManager: SyncManager,
    private val backupPreferences: BackupPreferences,
    private val restoreFromCloudUseCase: RestoreFromCloudUseCase,
    private val cloudAuthService: CloudAuthService,
    private val clipboard: PlatformClipboard,
) {
    private val _searchQuery = MutableStateFlow("")
    private val _openMenuItemId = MutableStateFlow<Long?>(null)
    private val _deleteConfirmation = MutableStateFlow<DeleteConfirmationState?>(null)

    // One-shot effects: buffer one item so emit() from a launched coroutine never suspends waiting
    // for a collector (e.g. when the screen is briefly not subscribed).
    private val _effect = MutableSharedFlow<HistoryEffect>(extraBufferCapacity = 1)
    val effect: SharedFlow<HistoryEffect> = _effect.asSharedFlow()

    private val _allItems = MutableStateFlow<List<HistoryItem>>(emptyList())
    private val _cloudCapacity = MutableStateFlow<com.socialvideodownloader.core.domain.sync.CloudCapacity?>(null)
    private val _isCloudBackupEnabled = MutableStateFlow(false)
    private val _syncStatus =
        MutableStateFlow<com.socialvideodownloader.core.domain.model.SyncStatus>(
            com.socialvideodownloader.core.domain.model.SyncStatus.Idle,
        )
    private val _restoreState = MutableStateFlow<RestoreState>(RestoreState.Idle)
    private val _isSignedIn = MutableStateFlow(cloudAuthService.isAuthenticated())
    private val _isSigningIn = MutableStateFlow(false)
    private val _hasSignInError = MutableStateFlow(false)

    val cloudBackupState: StateFlow<CloudBackupState> =
        combine(
            _isCloudBackupEnabled,
            _syncStatus,
            _restoreState,
            _isSignedIn,
            _isSigningIn,
            _hasSignInError,
        ) { values ->
            CloudBackupState(
                isCloudBackupEnabled = values[0] as Boolean,
                syncStatus = values[1] as com.socialvideodownloader.core.domain.model.SyncStatus,
                restoreState = values[2] as RestoreState,
                isSignedIn = values[3] as Boolean,
                isSigningIn = values[4] as Boolean,
                userName = if (values[3] as Boolean) cloudAuthService.getDisplayName() else null,
                userPhotoUrl = if (values[3] as Boolean) cloudAuthService.getPhotoUrl() else null,
                hasSignInError = values[5] as Boolean,
            )
        }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(5_000), CloudBackupState())

    init {
        downloadRepository.getAll()
            .map { records ->
                records.map { record ->
                    val contentUri = fileManager.resolveContentUri(record)
                    val isAccessible = contentUri?.let { fileManager.isFileAccessible(it) } ?: false
                    HistoryItem(
                        id = record.id,
                        title = record.videoTitle,
                        formatLabel = record.formatLabel,
                        thumbnailUrl = record.thumbnailUrl,
                        sourceUrl = record.sourceUrl,
                        status = record.status,
                        createdAt = record.createdAt,
                        fileSizeBytes = record.fileSizeBytes,
                        contentUri = contentUri,
                        isFileAccessible = isAccessible,
                    )
                }
            }
            .onEach { _allItems.value = it }
            .launchIn(coroutineScope)

        observeCloudCapacity()
            .onEach { _cloudCapacity.value = it }
            .launchIn(coroutineScope)

        backupPreferences.observeIsBackupEnabled()
            .onEach { _isCloudBackupEnabled.value = it }
            .launchIn(coroutineScope)

        syncManager.observeSyncStatus()
            .onEach { _syncStatus.value = it }
            .launchIn(coroutineScope)
    }

    val uiState: StateFlow<HistoryUiState> =
        combine(
            _allItems,
            _searchQuery,
            _openMenuItemId,
            _deleteConfirmation,
            _cloudCapacity,
        ) { allItems, query, openMenuItemId, deleteConfirmation, cloudCapacity ->
            val trimmedQuery = query.trim()
            if (allItems.isEmpty()) {
                HistoryUiState.Empty(query = trimmedQuery, isFiltering = false)
            } else {
                val filtered =
                    if (trimmedQuery.isBlank()) {
                        allItems
                    } else {
                        allItems.filter { it.title.contains(trimmedQuery, ignoreCase = true) }
                    }
                if (filtered.isEmpty()) {
                    HistoryUiState.Empty(query = trimmedQuery, isFiltering = true)
                } else {
                    HistoryUiState.Content(
                        query = trimmedQuery,
                        items = filtered.map { it.toListItem() },
                        openMenuItemId = openMenuItemId,
                        deleteConfirmation = deleteConfirmation,
                        cloudCapacity = cloudCapacity,
                    )
                }
            }
        }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState.Loading)

    fun onIntent(intent: HistoryIntent) {
        when (intent) {
            is HistoryIntent.SearchQueryChanged -> _searchQuery.value = intent.query
            is HistoryIntent.HistoryItemClicked -> handleItemClicked(intent.itemId)
            is HistoryIntent.ShareClicked -> handleShareClicked(intent.itemId)
            is HistoryIntent.CopyLinkClicked -> handleCopyLinkClicked(intent.itemId)
            is HistoryIntent.HistoryItemLongPressed -> _openMenuItemId.value = intent.itemId
            is HistoryIntent.DismissItemMenu -> _openMenuItemId.value = null
            is HistoryIntent.DeleteItemClicked -> handleDeleteItemClicked(intent.itemId)
            is HistoryIntent.DeleteFilesSelectionChanged ->
                handleDeleteFilesSelectionChanged(intent.selected)
            is HistoryIntent.ConfirmDeletion -> handleConfirmDeletion()
            is HistoryIntent.DismissDeletionDialog -> _deleteConfirmation.value = null
            is HistoryIntent.TapUpgrade -> handleTapUpgrade()
            is HistoryIntent.ToggleCloudBackup -> handleToggleCloudBackup()
            is HistoryIntent.SignInWithGoogle -> handleSignInWithGoogle(intent.idToken)
            is HistoryIntent.SignInCancelled -> _isSigningIn.value = false
            is HistoryIntent.SignInFailed -> handleSignInFailed()
            is HistoryIntent.SignOutCloud -> handleSignOut()
            is HistoryIntent.DismissSignInError -> _hasSignInError.value = false
            is HistoryIntent.RestoreFromCloud -> handleRestoreFromCloud()
            is HistoryIntent.DismissRestoreDialog -> _restoreState.value = RestoreState.Idle
        }
    }

    private fun handleRestoreFromCloud() {
        // Guard against a second restore starting while one is already running. Local dedup is an
        // in-memory snapshot per run, so concurrent restores could insert duplicate rows.
        if (_restoreState.value is RestoreState.InProgress) return
        coroutineScope.launch {
            _restoreState.value = RestoreState.InProgress(current = 0, total = 0)
            val result =
                restoreFromCloudUseCase { current, total ->
                    _restoreState.value = RestoreState.InProgress(current = current, total = total)
                }
            val restoreErrorReason = result.errorReason
            _restoreState.value =
                if (restoreErrorReason != null) {
                    RestoreState.Error(restoreErrorReason)
                } else {
                    RestoreState.Completed(restored = result.restored, skipped = result.skipped)
                }
        }
    }

    private fun handleToggleCloudBackup() {
        coroutineScope.launch {
            if (!_isSignedIn.value) {
                if (_isSigningIn.value) return@launch
                _hasSignInError.value = false
                _isSigningIn.value = true
                _effect.emit(HistoryEffect.LaunchGoogleSignIn)
            } else if (_isCloudBackupEnabled.value) {
                disableCloudBackupUseCase()
            } else {
                // Already authenticated — re-enable without re-running Google sign-in.
                // Reuses the use case so backfill/flush logic lives in exactly one place.
                enableCloudBackupUseCase()
            }
        }
    }

    private fun handleSignInWithGoogle(idToken: String) {
        coroutineScope.launch {
            _isSigningIn.value = true
            _hasSignInError.value = false
            try {
                enableCloudBackupUseCase(idToken)
                updateAuthState()
            } catch (e: Exception) {
                _hasSignInError.value = true
            } finally {
                _isSigningIn.value = false
            }
        }
    }

    private fun handleSignInFailed() {
        // The platform layer logs the underlying cause; the UI only needs to know a failure happened
        // so it can show a generic, localized message.
        _hasSignInError.value = true
        _isSigningIn.value = false
    }

    private fun handleSignOut() {
        coroutineScope.launch {
            disableCloudBackupUseCase()
            updateAuthState()
        }
    }

    private fun updateAuthState() {
        _isSignedIn.value = cloudAuthService.isAuthenticated()
    }

    private fun handleTapUpgrade() {
        coroutineScope.launch {
            _effect.emit(HistoryEffect.LaunchUpgradeFlow)
        }
    }

    /**
     * Surfaces the outcome of a platform purchase flow as a user-visible message. Kept here (not in
     * the platform delegate) so all history messaging stays in the shared layer. Cancellation is
     * silent — the user chose to back out.
     */
    fun onPurchaseResult(result: com.socialvideodownloader.core.domain.repository.BillingResult) {
        val messageType =
            when (result) {
                is com.socialvideodownloader.core.domain.repository.BillingResult.Success ->
                    HistoryMessageType.PURCHASE_SUCCESS
                is com.socialvideodownloader.core.domain.repository.BillingResult.Pending ->
                    HistoryMessageType.PURCHASE_PENDING
                is com.socialvideodownloader.core.domain.repository.BillingResult.Error ->
                    HistoryMessageType.PURCHASE_FAILED
                is com.socialvideodownloader.core.domain.repository.BillingResult.Cancelled -> return
            }
        coroutineScope.launch {
            _effect.emit(HistoryEffect.ShowMessage(messageType))
        }
    }

    private fun handleItemClicked(itemId: Long) {
        val item =
            (uiState.value as? HistoryUiState.Content)?.items?.find { it.id == itemId }
                ?: return
        coroutineScope.launch {
            when {
                item.status == DownloadStatus.FAILED ->
                    _effect.emit(HistoryEffect.RetryDownload(item.sourceUrl, existingRecordId = item.id))
                item.status == DownloadStatus.COMPLETED && item.isFileAccessible -> {
                    val uri = item.contentUri ?: return@launch
                    _effect.emit(HistoryEffect.OpenContent(uri))
                }
                item.status == DownloadStatus.COMPLETED && !item.isFileAccessible ->
                    _effect.emit(HistoryEffect.RetryDownload(item.sourceUrl, existingRecordId = item.id))
                else ->
                    _effect.emit(HistoryEffect.ShowMessage(HistoryMessageType.FILE_UNAVAILABLE))
            }
        }
    }

    private fun handleShareClicked(itemId: Long) {
        val item =
            (uiState.value as? HistoryUiState.Content)?.items?.find { it.id == itemId }
                ?: return
        coroutineScope.launch {
            if (item.isFileAccessible) {
                val uri = item.contentUri ?: return@launch
                _effect.emit(HistoryEffect.ShareContent(uri))
            } else {
                _effect.emit(HistoryEffect.ShowMessage(HistoryMessageType.FILE_UNAVAILABLE))
            }
        }
    }

    private fun handleCopyLinkClicked(itemId: Long) {
        val item =
            (uiState.value as? HistoryUiState.Content)?.items?.find { it.id == itemId }
                ?: return
        clipboard.copyToClipboard(item.sourceUrl)
        coroutineScope.launch {
            _effect.emit(HistoryEffect.ShowMessage(HistoryMessageType.COPY_URL_SUCCESS))
        }
    }

    private fun handleDeleteItemClicked(itemId: Long) {
        val item = _allItems.value.find { it.id == itemId } ?: return
        _deleteConfirmation.value =
            DeleteConfirmationState(
                target = DeleteTarget.Single(itemId),
                hasAnyAccessibleFile = item.isFileAccessible,
                affectedCount = 1,
            )
    }

    private fun handleDeleteFilesSelectionChanged(selected: Boolean) {
        val current = _deleteConfirmation.value ?: return
        _deleteConfirmation.value = current.copy(deleteFilesSelected = selected)
    }

    private fun handleConfirmDeletion() {
        val confirmation = _deleteConfirmation.value ?: return
        coroutineScope.launch {
            when (val target = confirmation.target) {
                is DeleteTarget.Single -> {
                    val result =
                        deleteHistoryItemUseCase(
                            target.itemId,
                            confirmation.deleteFilesSelected,
                        )
                    if (result.fileDeleteFailed) {
                        _effect.emit(HistoryEffect.ShowMessage(HistoryMessageType.DELETE_FILE_FAILED))
                    }
                }
            }
            _deleteConfirmation.value = null
        }
    }

    /** Cancel the coroutine scope when the ViewModel is cleared. */
    fun cleanup() {
        coroutineScope.cancel()
    }

    private fun HistoryItem.toListItem() =
        HistoryListItem(
            id = id,
            title = title,
            formatLabel = formatLabel,
            thumbnailUrl = thumbnailUrl,
            sourceUrl = sourceUrl,
            status = status,
            createdAt = createdAt,
            fileSizeBytes = fileSizeBytes,
            contentUri = contentUri,
            isFileAccessible = isFileAccessible,
        )
}
