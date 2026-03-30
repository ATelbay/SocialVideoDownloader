package com.socialvideodownloader.feature.history.ui

// Re-export shared KMP types so the existing Compose UI layer imports from
// com.socialvideodownloader.feature.history.ui without any changes.
typealias HistoryListItem = com.socialvideodownloader.shared.feature.history.HistoryListItem
typealias DeleteTarget = com.socialvideodownloader.shared.feature.history.DeleteTarget
typealias DeleteConfirmationState = com.socialvideodownloader.shared.feature.history.DeleteConfirmationState
typealias CloudBackupState = com.socialvideodownloader.shared.feature.history.CloudBackupState
typealias RestoreState = com.socialvideodownloader.shared.feature.history.RestoreState
typealias HistoryUiState = com.socialvideodownloader.shared.feature.history.HistoryUiState
