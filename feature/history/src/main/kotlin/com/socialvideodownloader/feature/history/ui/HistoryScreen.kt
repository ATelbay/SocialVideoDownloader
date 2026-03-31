package com.socialvideodownloader.feature.history.ui

import android.app.Activity
import android.text.format.DateUtils
import android.text.format.Formatter
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.socialvideodownloader.core.ui.util.openVideo
import com.socialvideodownloader.core.ui.util.shareVideo
import com.socialvideodownloader.feature.history.R
import com.socialvideodownloader.shared.feature.history.ui.HistoryScreen
import com.socialvideodownloader.shared.feature.history.ui.HistoryStrings
import kotlinx.coroutines.launch

@Composable
fun HistoryScreen(
    onNavigateToDownload: (initialUrl: String, existingRecordId: Long?) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = androidx.compose.runtime.remember { CredentialManager.create(context) }
    val googleWebClientId = stringResource(R.string.google_web_client_id)

    // Pre-resolve format strings for use in non-composable lambdas
    val capacityBannerFmt = stringResource(R.string.cloud_capacity_banner)
    val restoreProgressFmt = stringResource(R.string.cloud_restore_progress)
    val restoreCompletedFmt = stringResource(R.string.cloud_restore_complete)
    val cloudBackupSyncedFmt = stringResource(R.string.cloud_backup_synced)

    val strings = HistoryStrings(
        screenTitle = stringResource(R.string.history_screen_title_full),
        filterActionLabel = stringResource(R.string.history_action_filter),
        searchHint = stringResource(R.string.history_search_hint),
        emptyTitle = stringResource(R.string.history_empty_title),
        emptyDescription = stringResource(R.string.history_empty_description_new),
        noResultsDescription = stringResource(R.string.history_no_results_description),
        startDownloadingLabel = stringResource(R.string.history_start_downloading),
        startNewDownloadLabel = stringResource(R.string.history_start_new_download),
        restoreButtonLabel = stringResource(R.string.cloud_restore_button),
        capacityBannerText = { used, limit ->
            String.format(capacityBannerFmt, used, limit)
        },
        capacityUpgradeLabel = stringResource(R.string.cloud_capacity_upgrade),
        okLabel = stringResource(android.R.string.ok),
        restoreProgressText = { current, total ->
            String.format(restoreProgressFmt, current, total)
        },
        restoreCompletedText = { restored, skipped ->
            String.format(restoreCompletedFmt, restored, skipped)
        },
        restoreKeyLostText = stringResource(R.string.cloud_restore_key_lost),
        deleteTitle = stringResource(R.string.history_delete_single_title),
        deleteBodyText = stringResource(R.string.history_delete_message_single),
        deleteFilesLabel = stringResource(R.string.history_delete_checkbox_label),
        deleteCancelLabel = stringResource(R.string.history_delete_cancel),
        deleteConfirmLabel = stringResource(R.string.history_delete_confirm),
        bottomSheetCopyLinkLabel = stringResource(R.string.history_bottom_sheet_copy_link),
        bottomSheetShareLabel = stringResource(R.string.history_bottom_sheet_share),
        bottomSheetDeleteLabel = stringResource(R.string.history_bottom_sheet_delete),
        upgradeTitle = stringResource(R.string.upgrade_title),
        upgradeDescription = stringResource(R.string.upgrade_description),
        upgradePriceLabel = stringResource(R.string.upgrade_price),
        upgradeBuyLabel = stringResource(R.string.upgrade_buy_button),
        upgradeCancelLabel = stringResource(R.string.history_delete_cancel),
        cloudBackupToggleLabel = stringResource(R.string.cloud_backup_toggle_label),
        cloudSignInLabel = stringResource(R.string.cloud_sign_in_google),
        cloudSignOutLabel = stringResource(R.string.cloud_sign_out),
        cloudSignedInAs = stringResource(R.string.cloud_signed_in_as, ""),
        cloudSignInFailedMessage = stringResource(R.string.cloud_sign_in_failed),
        cloudBackupDisabledText = stringResource(R.string.cloud_backup_disabled),
        cloudBackupNeverText = stringResource(R.string.cloud_backup_never),
        cloudBackupSyncingText = stringResource(R.string.cloud_backup_syncing),
        cloudBackupSyncedText = { time -> String.format(cloudBackupSyncedFmt, time) },
        cloudBackupPausedText = stringResource(R.string.cloud_backup_paused),
        cloudBackupErrorText = stringResource(R.string.cloud_backup_error),
        msgDeleted = stringResource(R.string.history_deleted),
        msgAllDeleted = stringResource(R.string.history_all_deleted),
        msgLinkCopied = stringResource(R.string.history_link_copied),
        msgCloudSyncError = stringResource(R.string.history_cloud_sync_error),
        msgFileUnavailable = stringResource(R.string.history_file_unavailable),
        msgDeleteFileFailed = stringResource(R.string.history_delete_single_file_failed),
        msgOpenError = stringResource(R.string.history_open_error),
        msgShareError = stringResource(R.string.history_share_error),
    )

    HistoryScreen(
        viewModel = viewModel.shared,
        strings = strings,
        formattedDate = { epochMillis ->
            DateUtils.getRelativeTimeSpanString(
                epochMillis,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS,
            ).toString()
        },
        formattedSize = { bytes -> Formatter.formatFileSize(context, bytes) },
        onNavigateToDownload = onNavigateToDownload,
        onOpenFile = { uri -> context.openVideo(uri) },
        onShareFile = { uri -> context.shareVideo(uri) },
        onLaunchGoogleSignIn = {
            val activity = context as? Activity ?: return@HistoryScreen null
            try {
                val googleIdOption =
                    GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(false)
                        .setServerClientId(googleWebClientId)
                        .build()
                val request =
                    GetCredentialRequest.Builder()
                        .addCredentialOption(googleIdOption)
                        .build()
                val result = credentialManager.getCredential(context = activity, request = request)
                GoogleIdTokenCredential.createFrom(result.credential.data).idToken
            } catch (e: GetCredentialCancellationException) {
                null
            } catch (e: Exception) {
                Log.e("HistoryScreen", "Google sign-in failed", e)
                null
            }
        },
        onLaunchUpgradeFlow = {
            val activity = context as? Activity ?: return@HistoryScreen
            coroutineScope.launch { viewModel.launchPurchaseFlow(activity) }
        },
        modifier = modifier,
    )
}
