package com.socialvideodownloader.shared.feature.download.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.socialvideodownloader.shared.data.platform.DownloadErrorType
import com.socialvideodownloader.shared.feature.download.generated.resources.Res
import com.socialvideodownloader.shared.feature.download.generated.resources.action_new_download
import com.socialvideodownloader.shared.feature.download.generated.resources.action_retry
import com.socialvideodownloader.shared.feature.download.generated.resources.auth_connect_label
import com.socialvideodownloader.shared.feature.download.generated.resources.auth_reconnect_label
import com.socialvideodownloader.shared.feature.download.generated.resources.error_auth_body
import com.socialvideodownloader.shared.feature.download.generated.resources.error_auth_title
import com.socialvideodownloader.shared.feature.download.generated.resources.error_copyright_body
import com.socialvideodownloader.shared.feature.download.generated.resources.error_copyright_title
import com.socialvideodownloader.shared.feature.download.generated.resources.error_download_body
import com.socialvideodownloader.shared.feature.download.generated.resources.error_download_title
import com.socialvideodownloader.shared.feature.download.generated.resources.error_extraction_body
import com.socialvideodownloader.shared.feature.download.generated.resources.error_extraction_title
import com.socialvideodownloader.shared.feature.download.generated.resources.error_network_body
import com.socialvideodownloader.shared.feature.download.generated.resources.error_network_title
import com.socialvideodownloader.shared.feature.download.generated.resources.error_server_body
import com.socialvideodownloader.shared.feature.download.generated.resources.error_server_title
import com.socialvideodownloader.shared.feature.download.generated.resources.error_storage_body
import com.socialvideodownloader.shared.feature.download.generated.resources.error_storage_title
import com.socialvideodownloader.shared.feature.download.generated.resources.error_unknown_body
import com.socialvideodownloader.shared.feature.download.generated.resources.error_unknown_title
import com.socialvideodownloader.shared.feature.download.generated.resources.error_unsupported_body
import com.socialvideodownloader.shared.feature.download.generated.resources.error_unsupported_title
import com.socialvideodownloader.shared.network.auth.SupportedPlatform
import com.socialvideodownloader.shared.ui.components.GradientButton
import com.socialvideodownloader.shared.ui.components.TextActionLink
import com.socialvideodownloader.shared.ui.theme.Spacing
import com.socialvideodownloader.shared.ui.theme.SvdError
import com.socialvideodownloader.shared.ui.theme.SvdErrorSoft
import com.socialvideodownloader.shared.ui.theme.SvdForeground
import com.socialvideodownloader.shared.ui.theme.SvdMutedForeground
import org.jetbrains.compose.resources.stringResource

@Composable
fun DownloadErrorContent(
    errorType: DownloadErrorType,
    message: String?,
    onRetryClicked: () -> Unit,
    onNewDownloadClicked: () -> Unit,
    modifier: Modifier = Modifier,
    platformForAuth: SupportedPlatform? = null,
    isReconnect: Boolean = false,
    onConnectPlatformClicked: (SupportedPlatform) -> Unit = {},
) {
    val title =
        when (errorType) {
            DownloadErrorType.NETWORK_ERROR -> stringResource(Res.string.error_network_title)
            DownloadErrorType.SERVER_UNAVAILABLE -> stringResource(Res.string.error_server_title)
            DownloadErrorType.EXTRACTION_FAILED -> stringResource(Res.string.error_extraction_title)
            DownloadErrorType.UNSUPPORTED_URL -> stringResource(Res.string.error_unsupported_title)
            DownloadErrorType.STORAGE_FULL -> stringResource(Res.string.error_storage_title)
            DownloadErrorType.DOWNLOAD_FAILED -> stringResource(Res.string.error_download_title)
            DownloadErrorType.AUTH_REQUIRED -> stringResource(Res.string.error_auth_title)
            DownloadErrorType.COPYRIGHT -> stringResource(Res.string.error_copyright_title)
            DownloadErrorType.UNKNOWN -> stringResource(Res.string.error_unknown_title)
        }

    val fallbackBody =
        when (errorType) {
            DownloadErrorType.NETWORK_ERROR -> stringResource(Res.string.error_network_body)
            DownloadErrorType.SERVER_UNAVAILABLE -> stringResource(Res.string.error_server_body)
            DownloadErrorType.EXTRACTION_FAILED -> stringResource(Res.string.error_extraction_body)
            DownloadErrorType.UNSUPPORTED_URL -> stringResource(Res.string.error_unsupported_body)
            DownloadErrorType.STORAGE_FULL -> stringResource(Res.string.error_storage_body)
            DownloadErrorType.DOWNLOAD_FAILED -> stringResource(Res.string.error_download_body)
            DownloadErrorType.AUTH_REQUIRED -> stringResource(Res.string.error_auth_body)
            DownloadErrorType.COPYRIGHT -> stringResource(Res.string.error_copyright_body)
            DownloadErrorType.UNKNOWN -> stringResource(Res.string.error_unknown_body)
        }

    val body =
        message
            ?.takeUnless { it.isBlank() || it == errorType.name }
            ?: fallbackBody

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(Spacing.HeroIconSize)
                        .background(SvdErrorSoft, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Warning,
                    contentDescription = null,
                    tint = SvdError,
                    modifier = Modifier.size(40.dp),
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = SvdForeground,
                textAlign = TextAlign.Center,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = SvdMutedForeground,
                textAlign = TextAlign.Center,
            )
        }

        if (errorType == DownloadErrorType.AUTH_REQUIRED && platformForAuth != null) {
            GradientButton(
                text =
                    if (isReconnect) {
                        stringResource(Res.string.auth_reconnect_label, platformForAuth.displayName)
                    } else {
                        stringResource(Res.string.auth_connect_label, platformForAuth.displayName)
                    },
                onClick = { onConnectPlatformClicked(platformForAuth) },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        GradientButton(
            text = stringResource(Res.string.action_retry),
            onClick = onRetryClicked,
            icon = Icons.Outlined.Refresh,
            modifier = Modifier.fillMaxWidth(),
        )

        TextActionLink(
            text = stringResource(Res.string.action_new_download),
            onClick = onNewDownloadClicked,
        )
    }
}
