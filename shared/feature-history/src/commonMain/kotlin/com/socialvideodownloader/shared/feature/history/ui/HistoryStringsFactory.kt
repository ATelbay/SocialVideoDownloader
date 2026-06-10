package com.socialvideodownloader.shared.feature.history.ui

import androidx.compose.runtime.Composable
import com.socialvideodownloader.shared.feature.history.generated.resources.Res
import com.socialvideodownloader.shared.feature.history.generated.resources.cloud_backup_disabled
import com.socialvideodownloader.shared.feature.history.generated.resources.cloud_backup_error
import com.socialvideodownloader.shared.feature.history.generated.resources.cloud_backup_never
import com.socialvideodownloader.shared.feature.history.generated.resources.cloud_backup_paused
import com.socialvideodownloader.shared.feature.history.generated.resources.cloud_backup_synced
import com.socialvideodownloader.shared.feature.history.generated.resources.cloud_backup_syncing
import com.socialvideodownloader.shared.feature.history.generated.resources.cloud_backup_toggle_label
import com.socialvideodownloader.shared.feature.history.generated.resources.cloud_capacity_banner
import com.socialvideodownloader.shared.feature.history.generated.resources.cloud_capacity_upgrade
import com.socialvideodownloader.shared.feature.history.generated.resources.cloud_restore_button
import com.socialvideodownloader.shared.feature.history.generated.resources.cloud_restore_complete
import com.socialvideodownloader.shared.feature.history.generated.resources.cloud_restore_error
import com.socialvideodownloader.shared.feature.history.generated.resources.cloud_restore_key_lost
import com.socialvideodownloader.shared.feature.history.generated.resources.cloud_restore_progress
import com.socialvideodownloader.shared.feature.history.generated.resources.cloud_sign_in
import com.socialvideodownloader.shared.feature.history.generated.resources.cloud_sign_in_failed
import com.socialvideodownloader.shared.feature.history.generated.resources.cloud_sign_out
import com.socialvideodownloader.shared.feature.history.generated.resources.cloud_signed_in
import com.socialvideodownloader.shared.feature.history.generated.resources.common_ok
import com.socialvideodownloader.shared.feature.history.generated.resources.history_bottom_sheet_copy_link
import com.socialvideodownloader.shared.feature.history.generated.resources.history_bottom_sheet_delete
import com.socialvideodownloader.shared.feature.history.generated.resources.history_bottom_sheet_share
import com.socialvideodownloader.shared.feature.history.generated.resources.history_clear_search
import com.socialvideodownloader.shared.feature.history.generated.resources.history_cloud_sync_error
import com.socialvideodownloader.shared.feature.history.generated.resources.history_delete_body
import com.socialvideodownloader.shared.feature.history.generated.resources.history_delete_cancel
import com.socialvideodownloader.shared.feature.history.generated.resources.history_delete_confirm
import com.socialvideodownloader.shared.feature.history.generated.resources.history_delete_file_failed
import com.socialvideodownloader.shared.feature.history.generated.resources.history_delete_files_label
import com.socialvideodownloader.shared.feature.history.generated.resources.history_delete_title
import com.socialvideodownloader.shared.feature.history.generated.resources.history_deleted
import com.socialvideodownloader.shared.feature.history.generated.resources.history_empty_description
import com.socialvideodownloader.shared.feature.history.generated.resources.history_empty_title
import com.socialvideodownloader.shared.feature.history.generated.resources.history_file_unavailable
import com.socialvideodownloader.shared.feature.history.generated.resources.history_filter_action
import com.socialvideodownloader.shared.feature.history.generated.resources.history_link_copied
import com.socialvideodownloader.shared.feature.history.generated.resources.history_no_results_description
import com.socialvideodownloader.shared.feature.history.generated.resources.history_open_error
import com.socialvideodownloader.shared.feature.history.generated.resources.history_purchase_failed
import com.socialvideodownloader.shared.feature.history.generated.resources.history_purchase_pending
import com.socialvideodownloader.shared.feature.history.generated.resources.history_purchase_success
import com.socialvideodownloader.shared.feature.history.generated.resources.history_screen_title
import com.socialvideodownloader.shared.feature.history.generated.resources.history_search_hint
import com.socialvideodownloader.shared.feature.history.generated.resources.history_share_error
import com.socialvideodownloader.shared.feature.history.generated.resources.history_start_downloading
import com.socialvideodownloader.shared.feature.history.generated.resources.history_start_new_download
import com.socialvideodownloader.shared.feature.history.generated.resources.upgrade_buy_button
import com.socialvideodownloader.shared.feature.history.generated.resources.upgrade_description
import com.socialvideodownloader.shared.feature.history.generated.resources.upgrade_price
import com.socialvideodownloader.shared.feature.history.generated.resources.upgrade_title
import org.jetbrains.compose.resources.stringResource

/**
 * Builds [HistoryStrings] from the shared Compose Multiplatform string resources so that
 * Android and the iOS shell read from a single source of truth.
 *
 * Format-arg fields are exposed as lambdas, which are invoked outside composition. Compose's
 * [stringResource] is `@Composable`, so the raw templates are resolved here and the positional
 * placeholders are substituted at call time via [formatPositional] (commonMain has no
 * `String.format`).
 */
@Composable
fun rememberHistoryStrings(): HistoryStrings {
    val capacityBannerTemplate = stringResource(Res.string.cloud_capacity_banner)
    val restoreProgressTemplate = stringResource(Res.string.cloud_restore_progress)
    val restoreCompletedTemplate = stringResource(Res.string.cloud_restore_complete)
    val cloudBackupSyncedTemplate = stringResource(Res.string.cloud_backup_synced)

    return HistoryStrings(
        screenTitle = stringResource(Res.string.history_screen_title),
        filterActionLabel = stringResource(Res.string.history_filter_action),
        searchHint = stringResource(Res.string.history_search_hint),
        clearSearchLabel = stringResource(Res.string.history_clear_search),
        emptyTitle = stringResource(Res.string.history_empty_title),
        emptyDescription = stringResource(Res.string.history_empty_description),
        noResultsDescription = stringResource(Res.string.history_no_results_description),
        startDownloadingLabel = stringResource(Res.string.history_start_downloading),
        startNewDownloadLabel = stringResource(Res.string.history_start_new_download),
        restoreButtonLabel = stringResource(Res.string.cloud_restore_button),
        capacityBannerText = { used, limit -> capacityBannerTemplate.formatPositional(used, limit) },
        capacityUpgradeLabel = stringResource(Res.string.cloud_capacity_upgrade),
        okLabel = stringResource(Res.string.common_ok),
        restoreProgressText = { current, total -> restoreProgressTemplate.formatPositional(current, total) },
        restoreCompletedText = { restored, skipped -> restoreCompletedTemplate.formatPositional(restored, skipped) },
        restoreKeyLostText = stringResource(Res.string.cloud_restore_key_lost),
        restoreErrorText = stringResource(Res.string.cloud_restore_error),
        deleteTitle = stringResource(Res.string.history_delete_title),
        deleteBodyText = stringResource(Res.string.history_delete_body),
        deleteFilesLabel = stringResource(Res.string.history_delete_files_label),
        deleteCancelLabel = stringResource(Res.string.history_delete_cancel),
        deleteConfirmLabel = stringResource(Res.string.history_delete_confirm),
        bottomSheetCopyLinkLabel = stringResource(Res.string.history_bottom_sheet_copy_link),
        bottomSheetShareLabel = stringResource(Res.string.history_bottom_sheet_share),
        bottomSheetDeleteLabel = stringResource(Res.string.history_bottom_sheet_delete),
        upgradeTitle = stringResource(Res.string.upgrade_title),
        upgradeDescription = stringResource(Res.string.upgrade_description),
        upgradePriceLabel = stringResource(Res.string.upgrade_price),
        upgradeBuyLabel = stringResource(Res.string.upgrade_buy_button),
        upgradeCancelLabel = stringResource(Res.string.history_delete_cancel),
        cloudBackupToggleLabel = stringResource(Res.string.cloud_backup_toggle_label),
        cloudSignInLabel = stringResource(Res.string.cloud_sign_in),
        cloudSignOutLabel = stringResource(Res.string.cloud_sign_out),
        cloudSignedInAs = stringResource(Res.string.cloud_signed_in),
        cloudSignInFailedMessage = stringResource(Res.string.cloud_sign_in_failed),
        cloudBackupDisabledText = stringResource(Res.string.cloud_backup_disabled),
        cloudBackupNeverText = stringResource(Res.string.cloud_backup_never),
        cloudBackupSyncingText = stringResource(Res.string.cloud_backup_syncing),
        cloudBackupSyncedText = { time -> cloudBackupSyncedTemplate.formatPositional(time) },
        cloudBackupPausedText = stringResource(Res.string.cloud_backup_paused),
        cloudBackupErrorText = stringResource(Res.string.cloud_backup_error),
        msgDeleted = stringResource(Res.string.history_deleted),
        msgLinkCopied = stringResource(Res.string.history_link_copied),
        msgCloudSyncError = stringResource(Res.string.history_cloud_sync_error),
        msgFileUnavailable = stringResource(Res.string.history_file_unavailable),
        msgDeleteFileFailed = stringResource(Res.string.history_delete_file_failed),
        msgOpenError = stringResource(Res.string.history_open_error),
        msgShareError = stringResource(Res.string.history_share_error),
        msgPurchaseSuccess = stringResource(Res.string.history_purchase_success),
        msgPurchasePending = stringResource(Res.string.history_purchase_pending),
        msgPurchaseFailed = stringResource(Res.string.history_purchase_failed),
    )
}

/**
 * Substitutes positional placeholders (`%1$d`, `%2$d`, `%1$s`, …) in an Android-style format
 * string. Mirrors the subset of `String.format` behaviour the resource templates rely on, which
 * is unavailable in commonMain.
 */
private fun String.formatPositional(vararg args: Any): String {
    var result = this
    args.forEachIndexed { index, arg ->
        val position = index + 1
        val value = arg.toString()
        result =
            result
                .replace("%$position\$d", value)
                .replace("%$position\$s", value)
    }
    return result
}
