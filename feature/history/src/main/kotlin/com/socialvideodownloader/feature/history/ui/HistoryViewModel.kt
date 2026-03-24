package com.socialvideodownloader.feature.history.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.socialvideodownloader.core.domain.model.DownloadStatus
import com.socialvideodownloader.core.domain.model.HistoryItem
import com.socialvideodownloader.core.domain.model.SyncStatus
import com.socialvideodownloader.core.domain.repository.BillingRepository
import com.socialvideodownloader.core.domain.sync.BackupPreferences
import com.socialvideodownloader.core.domain.sync.CloudAuthService
import com.socialvideodownloader.core.domain.sync.CloudCapacity
import com.socialvideodownloader.core.domain.sync.DisableCloudBackupUseCase
import com.socialvideodownloader.core.domain.sync.EnableCloudBackupUseCase
import com.socialvideodownloader.core.domain.sync.ObserveCloudCapacityUseCase
import com.socialvideodownloader.core.domain.sync.RestoreFromCloudUseCase
import com.socialvideodownloader.core.domain.sync.SyncManager
import com.socialvideodownloader.feature.history.R
import com.socialvideodownloader.feature.history.domain.DeleteHistoryItemUseCase
import com.socialvideodownloader.feature.history.domain.ObserveHistoryItemsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val observeHistoryItems: ObserveHistoryItemsUseCase,
    private val deleteHistoryItem: DeleteHistoryItemUseCase,
    // US3: Billing — capacity observation and purchase flow
    private val observeCloudCapacity: ObserveCloudCapacityUseCase,
    private val billingRepository: BillingRepository,
    // US1: Cloud backup toggle
    private val enableCloudBackupUseCase: EnableCloudBackupUseCase,
    private val disableCloudBackupUseCase: DisableCloudBackupUseCase,
    private val syncManager: SyncManager,
    private val backupPreferences: BackupPreferences,
    // US2: Restore from cloud
    private val restoreFromCloudUseCase: RestoreFromCloudUseCase,
    // Google Sign-In — read-only display info
    private val cloudAuthService: CloudAuthService,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _openMenuItemId = MutableStateFlow<Long?>(null)
    private val _deleteConfirmation = MutableStateFlow<DeleteConfirmationState?>(null)

    private val _effect = MutableSharedFlow<HistoryEffect>()
    val effect: SharedFlow<HistoryEffect> = _effect.asSharedFlow()

    // Keep a reference to all items for delete operations that need access to the full list
    private val _allItems = MutableStateFlow<List<HistoryItem>>(emptyList())

    // US3: Billing — cloud capacity state
    private val _cloudCapacity = MutableStateFlow<CloudCapacity?>(null)

    // US1: Cloud backup state
    private val _isCloudBackupEnabled = MutableStateFlow(false)
    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)

    // US2: Restore state
    private val _restoreState = MutableStateFlow<RestoreState>(RestoreState.Idle)

    // Google Sign-In state
    private val _isSignedIn = MutableStateFlow(cloudAuthService.isAuthenticated())
    private val _isSigningIn = MutableStateFlow(false)
    private val _signInError = MutableStateFlow<String?>(null)

    val cloudBackupState: StateFlow<CloudBackupState> = combine(
        _isCloudBackupEnabled,
        _syncStatus,
        _restoreState,
        _isSignedIn,
        _isSigningIn,
        _signInError,
    ) { values ->
        CloudBackupState(
            isCloudBackupEnabled = values[0] as Boolean,
            syncStatus = values[1] as SyncStatus,
            restoreState = values[2] as RestoreState,
            isSignedIn = values[3] as Boolean,
            isSigningIn = values[4] as Boolean,
            userName = if (values[3] as Boolean) cloudAuthService.getDisplayName() else null,
            userPhotoUrl = if (values[3] as Boolean) cloudAuthService.getPhotoUrl() else null,
            signInError = values[5] as String?,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CloudBackupState())

    init {
        observeHistoryItems()
            .onEach { _allItems.value = it }
            .launchIn(viewModelScope)

        // US3: Billing — observe cloud capacity for banner
        observeCloudCapacity()
            .onEach { _cloudCapacity.value = it }
            .launchIn(viewModelScope)

        // US1: Observe cloud backup preferences and sync status
        backupPreferences.observeIsBackupEnabled()
            .onEach { _isCloudBackupEnabled.value = it }
            .launchIn(viewModelScope)

        syncManager.observeSyncStatus()
            .onEach { _syncStatus.value = it }
            .launchIn(viewModelScope)
    }

    val uiState: StateFlow<HistoryUiState> = combine(
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
            val filtered = if (trimmedQuery.isBlank()) {
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
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState.Loading)

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
            // US3: Billing — launch upgrade purchase flow
            is HistoryIntent.TapUpgrade -> handleTapUpgrade()
            // US1: Cloud backup toggle
            is HistoryIntent.ToggleCloudBackup -> handleToggleCloudBackup()
            // Google Sign-In
            is HistoryIntent.SignInWithGoogle -> handleSignInWithGoogle(intent.idToken)
            is HistoryIntent.SignOutCloud -> handleSignOut()
            is HistoryIntent.DismissSignInError -> _signInError.value = null
            // US2: Restore from cloud
            is HistoryIntent.RestoreFromCloud -> handleRestoreFromCloud()
            is HistoryIntent.DismissRestoreDialog -> _restoreState.value = RestoreState.Idle
        }
    }

    private fun handleRestoreFromCloud() {
        viewModelScope.launch {
            _restoreState.value = RestoreState.InProgress(current = 0, total = 0)
            val result = restoreFromCloudUseCase { current, total ->
                _restoreState.value = RestoreState.InProgress(current = current, total = total)
            }
            val restoreError = result.error
            _restoreState.value = if (restoreError != null) {
                RestoreState.Error(restoreError)
            } else {
                RestoreState.Completed(restored = result.restored, skipped = result.skipped)
            }
        }
    }

    private fun handleToggleCloudBackup() {
        viewModelScope.launch {
            if (!_isSignedIn.value) {
                _effect.emit(HistoryEffect.LaunchGoogleSignIn)
            } else if (_isCloudBackupEnabled.value) {
                disableCloudBackupUseCase()
            } else {
                // Already signed in, just re-enable backup
                backupPreferences.setBackupEnabled(true)
                backupPreferences.setHasEverEnabled(true)
                syncManager.processPendingOperations()
            }
        }
    }

    private fun handleSignInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _isSigningIn.value = true
            _signInError.value = null
            try {
                enableCloudBackupUseCase(idToken)
                updateAuthState()
            } catch (e: Exception) {
                android.util.Log.e("HistoryViewModel", "Google sign-in failed", e)
                _signInError.value = e.message
            } finally {
                _isSigningIn.value = false
            }
        }
    }

    private fun handleSignOut() {
        viewModelScope.launch {
            disableCloudBackupUseCase()
            updateAuthState()
        }
    }

    private fun updateAuthState() {
        _isSignedIn.value = cloudAuthService.isAuthenticated()
    }

    private fun handleTapUpgrade() {
        viewModelScope.launch {
            _effect.emit(HistoryEffect.LaunchUpgradeFlow)
        }
    }

    /** Called by the screen with the Activity reference required by Google Play Billing. */
    suspend fun launchPurchaseFlow(activity: Any) {
        billingRepository.launchPurchaseFlow(activity)
    }

    private fun handleItemClicked(itemId: Long) {
        val item = (uiState.value as? HistoryUiState.Content)?.items?.find { it.id == itemId }
            ?: return
        viewModelScope.launch {
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
                    _effect.emit(HistoryEffect.ShowMessage(R.string.history_file_unavailable))
            }
        }
    }

    private fun handleShareClicked(itemId: Long) {
        val item = (uiState.value as? HistoryUiState.Content)?.items?.find { it.id == itemId }
            ?: return
        viewModelScope.launch {
            if (item.isFileAccessible) {
                val uri = item.contentUri ?: return@launch
                _effect.emit(HistoryEffect.ShareContent(uri))
            } else {
                _effect.emit(HistoryEffect.ShowMessage(R.string.history_file_unavailable))
            }
        }
    }

    private fun handleCopyLinkClicked(itemId: Long) {
        val item = (uiState.value as? HistoryUiState.Content)?.items?.find { it.id == itemId }
            ?: return
        val clipboard = appContext.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        clipboard.setPrimaryClip(ClipData.newPlainText(null, item.sourceUrl))
        viewModelScope.launch {
            _effect.emit(HistoryEffect.ShowMessage(R.string.history_link_copied))
        }
    }

    private fun handleDeleteItemClicked(itemId: Long) {
        val item = _allItems.value.find { it.id == itemId } ?: return
        _deleteConfirmation.value = DeleteConfirmationState(
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
        viewModelScope.launch {
            when (val target = confirmation.target) {
                is DeleteTarget.Single -> {
                    val result =
                        deleteHistoryItem(target.itemId, confirmation.deleteFilesSelected)
                    if (result.fileDeleteFailed) {
                        _effect.emit(
                            HistoryEffect.ShowMessage(R.string.history_delete_single_file_failed),
                        )
                    }
                }
            }
            _deleteConfirmation.value = null
        }
    }

    private fun HistoryItem.toListItem() = HistoryListItem(
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
