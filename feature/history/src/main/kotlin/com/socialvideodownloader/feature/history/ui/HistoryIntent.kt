package com.socialvideodownloader.feature.history.ui

sealed interface HistoryIntent {
    data class SearchQueryChanged(val query: String) : HistoryIntent
    data class HistoryItemClicked(val itemId: Long) : HistoryIntent
    data class HistoryItemLongPressed(val itemId: Long) : HistoryIntent
    data object DismissItemMenu : HistoryIntent
    data class ShareClicked(val itemId: Long) : HistoryIntent
    data class CopyLinkClicked(val itemId: Long) : HistoryIntent
    data class DeleteItemClicked(val itemId: Long) : HistoryIntent
    data class DeleteFilesSelectionChanged(val selected: Boolean) : HistoryIntent
    data object ConfirmDeletion : HistoryIntent
    data object DismissDeletionDialog : HistoryIntent
    // US3: Billing — upgrade tap intent
    data object TapUpgrade : HistoryIntent
    // US1: Cloud backup toggle
    data object ToggleCloudBackup : HistoryIntent
    // Google Sign-In
    data class SignInWithGoogle(val idToken: String) : HistoryIntent
    data object SignOutCloud : HistoryIntent
    data object DismissSignInError : HistoryIntent
    // US2: Restore from cloud
    data object RestoreFromCloud : HistoryIntent
    data object DismissRestoreDialog : HistoryIntent
}
