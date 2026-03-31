import Foundation
@preconcurrency import shared_feature_library

// Bridges the feature-library exported Swift names back to the simpler
// download/history type names the iOS UI was already written against.

// MARK: - Download aliases

typealias SharedDownloadViewModel = Feature_downloadSharedDownloadViewModel

typealias DownloadUiState = Feature_downloadDownloadUiState
typealias DownloadUiStateIdle = Feature_downloadDownloadUiStateIdle
typealias DownloadUiStateExtracting = Feature_downloadDownloadUiStateExtracting
typealias DownloadUiStateFormatSelection = Feature_downloadDownloadUiStateFormatSelection
typealias DownloadUiStateDownloading = Feature_downloadDownloadUiStateDownloading
typealias DownloadUiStateDone = Feature_downloadDownloadUiStateDone
typealias DownloadUiStateError = Feature_downloadDownloadUiStateError

typealias DownloadIntent = Feature_downloadDownloadIntent
typealias DownloadIntentBackToIdleClicked = Feature_downloadDownloadIntentBackToIdleClicked
typealias DownloadIntentCancelDownloadClicked = Feature_downloadDownloadIntentCancelDownloadClicked
typealias DownloadIntentDismissExistingBanner = Feature_downloadDownloadIntentDismissExistingBanner
typealias DownloadIntentDownloadClicked = Feature_downloadDownloadIntentDownloadClicked
typealias DownloadIntentExtractClicked = Feature_downloadDownloadIntentExtractClicked
typealias DownloadIntentFormatSelected = Feature_downloadDownloadIntentFormatSelected
typealias DownloadIntentNewDownloadClicked = Feature_downloadDownloadIntentNewDownloadClicked
typealias DownloadIntentOpenExistingClicked = Feature_downloadDownloadIntentOpenExistingClicked
typealias DownloadIntentOpenFileClicked = Feature_downloadDownloadIntentOpenFileClicked
typealias DownloadIntentRetryClicked = Feature_downloadDownloadIntentRetryClicked
typealias DownloadIntentShareExistingClicked = Feature_downloadDownloadIntentShareExistingClicked
typealias DownloadIntentShareFileClicked = Feature_downloadDownloadIntentShareFileClicked
typealias DownloadIntentUrlChanged = Feature_downloadDownloadIntentUrlChanged

typealias DownloadEventOpenFile = Feature_downloadDownloadEventOpenFile
typealias DownloadEventShareFile = Feature_downloadDownloadEventShareFile
typealias DownloadEventShowError = Feature_downloadDownloadEventShowError

// MARK: - History aliases

typealias SharedHistoryViewModel = Feature_historySharedHistoryViewModel

typealias HistoryUiState = Feature_historyHistoryUiState
typealias HistoryUiStateLoading = Feature_historyHistoryUiStateLoading
typealias HistoryUiStateEmpty = Feature_historyHistoryUiStateEmpty
typealias HistoryUiStateContent = Feature_historyHistoryUiStateContent

typealias HistoryIntent = Feature_historyHistoryIntent
typealias HistoryIntentConfirmDeletion = Feature_historyHistoryIntentConfirmDeletion
typealias HistoryIntentCopyLinkClicked = Feature_historyHistoryIntentCopyLinkClicked
typealias HistoryIntentDeleteItemClicked = Feature_historyHistoryIntentDeleteItemClicked
typealias HistoryIntentDismissDeletionDialog = Feature_historyHistoryIntentDismissDeletionDialog
typealias HistoryIntentHistoryItemClicked = Feature_historyHistoryIntentHistoryItemClicked
typealias HistoryIntentRestoreFromCloud = Feature_historyHistoryIntentRestoreFromCloud
typealias HistoryIntentSearchQueryChanged = Feature_historyHistoryIntentSearchQueryChanged
typealias HistoryIntentShareClicked = Feature_historyHistoryIntentShareClicked
typealias HistoryIntentSignOutCloud = Feature_historyHistoryIntentSignOutCloud
typealias HistoryIntentTapUpgrade = Feature_historyHistoryIntentTapUpgrade
typealias HistoryIntentToggleCloudBackup = Feature_historyHistoryIntentToggleCloudBackup

typealias HistoryEffectLaunchGoogleSignIn = Feature_historyHistoryEffectLaunchGoogleSignIn
typealias HistoryEffectLaunchUpgradeFlow = Feature_historyHistoryEffectLaunchUpgradeFlow
typealias HistoryEffectOpenContent = Feature_historyHistoryEffectOpenContent
typealias HistoryEffectRetryDownload = Feature_historyHistoryEffectRetryDownload
typealias HistoryEffectShareContent = Feature_historyHistoryEffectShareContent
typealias HistoryEffectShowMessage = Feature_historyHistoryEffectShowMessage

typealias HistoryListItem = Feature_historyHistoryListItem
typealias CloudBackupState = Feature_historyCloudBackupState
typealias DeleteConfirmationState = Feature_historyDeleteConfirmationState

typealias RestoreState = Feature_historyRestoreState
typealias RestoreStateCompleted = Feature_historyRestoreStateCompleted
typealias RestoreStateError = Feature_historyRestoreStateError
typealias RestoreStateIdle = Feature_historyRestoreStateIdle
typealias RestoreStateInProgress = Feature_historyRestoreStateInProgress

/// Swift-side wrapper around the Kotlin KoinHelper object.
///
/// Provides typed factory methods that are more idiomatic Swift
/// than calling the Kotlin object directly.
enum KoinViewModelFactory {

    /// Create a new SharedDownloadViewModel.
    /// Caller is responsible for calling `vm.cleanup()` on disappear.
    static func makeDownloadViewModel() -> SharedDownloadViewModel {
        KoinHelper.shared.getDownloadViewModel()
    }

    /// Create a new SharedHistoryViewModel.
    static func makeHistoryViewModel() -> SharedHistoryViewModel {
        KoinHelper.shared.getHistoryViewModel()
    }

    /// Create a new SharedLibraryViewModel.
    static func makeLibraryViewModel() -> SharedLibraryViewModel {
        KoinHelper.shared.getLibraryViewModel()
    }
}
