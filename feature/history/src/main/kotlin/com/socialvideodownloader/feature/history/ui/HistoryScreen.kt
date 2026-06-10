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
import com.socialvideodownloader.shared.feature.history.ui.GoogleSignInResult
import com.socialvideodownloader.shared.feature.history.ui.HistoryScreen
import com.socialvideodownloader.shared.feature.history.ui.rememberHistoryStrings
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

    HistoryScreen(
        viewModel = viewModel.shared,
        strings = rememberHistoryStrings(),
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
            val activity =
                context as? Activity
                    ?: run {
                        Log.e("HistoryScreen", "Google sign-in launched without a host Activity")
                        return@HistoryScreen GoogleSignInResult.Failed
                    }
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
                val idToken = GoogleIdTokenCredential.createFrom(result.credential.data).idToken
                GoogleSignInResult.Success(idToken)
            } catch (e: GetCredentialCancellationException) {
                GoogleSignInResult.Cancelled
            } catch (e: Exception) {
                Log.e("HistoryScreen", "Google sign-in failed", e)
                GoogleSignInResult.Failed
            }
        },
        onLaunchUpgradeFlow = {
            val activity = context as? Activity ?: return@HistoryScreen
            coroutineScope.launch { viewModel.launchPurchaseFlow(activity) }
        },
        modifier = modifier,
    )
}
