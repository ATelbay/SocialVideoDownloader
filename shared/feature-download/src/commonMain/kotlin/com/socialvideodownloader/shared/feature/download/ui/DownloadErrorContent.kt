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
import com.socialvideodownloader.shared.network.auth.SupportedPlatform
import com.socialvideodownloader.shared.ui.components.GradientButton
import com.socialvideodownloader.shared.ui.components.TextActionLink
import com.socialvideodownloader.shared.ui.theme.Spacing
import com.socialvideodownloader.shared.ui.theme.SvdError
import com.socialvideodownloader.shared.ui.theme.SvdErrorSoft
import com.socialvideodownloader.shared.ui.theme.SvdForeground
import com.socialvideodownloader.shared.ui.theme.SvdMutedForeground

@Composable
fun DownloadErrorContent(
    errorType: DownloadErrorType,
    message: String?,
    onRetryClicked: () -> Unit,
    onNewDownloadClicked: () -> Unit,
    modifier: Modifier = Modifier,
    platformForAuth: SupportedPlatform? = null,
    onConnectPlatformClicked: (SupportedPlatform) -> Unit = {},
) {
    val (title, body) = errorPresentation(errorType = errorType, message = message)

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
                text = DownloadAuthStrings.connectLabel(platformForAuth.displayName),
                onClick = { onConnectPlatformClicked(platformForAuth) },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        GradientButton(
            text = "Retry",
            onClick = onRetryClicked,
            icon = Icons.Outlined.Refresh,
            modifier = Modifier.fillMaxWidth(),
        )

        TextActionLink(
            text = "New Download",
            onClick = onNewDownloadClicked,
        )
    }
}

private fun errorPresentation(
    errorType: DownloadErrorType,
    message: String?,
): Pair<String, String> {
    val fallbackBody =
        when (errorType) {
            DownloadErrorType.NETWORK_ERROR -> "Network error. Check your connection and try again."
            DownloadErrorType.SERVER_UNAVAILABLE -> "Download server is unavailable. Try again later."
            DownloadErrorType.EXTRACTION_FAILED -> "Could not extract video info. The video may be private or unavailable."
            DownloadErrorType.UNSUPPORTED_URL -> "This URL is not supported."
            DownloadErrorType.STORAGE_FULL -> "Not enough storage space to save the download."
            DownloadErrorType.DOWNLOAD_FAILED -> "Download failed. Please try again."
            DownloadErrorType.AUTH_REQUIRED -> "Authentication required to download this content."
            DownloadErrorType.UNKNOWN -> "Something went wrong. Please try again."
        }

    val title =
        when (errorType) {
            DownloadErrorType.NETWORK_ERROR -> "Network error"
            DownloadErrorType.SERVER_UNAVAILABLE -> "Server unavailable"
            DownloadErrorType.EXTRACTION_FAILED -> "Failed to extract"
            DownloadErrorType.UNSUPPORTED_URL -> "Unsupported URL"
            DownloadErrorType.STORAGE_FULL -> "Storage full"
            DownloadErrorType.DOWNLOAD_FAILED -> "Download failed"
            DownloadErrorType.AUTH_REQUIRED -> "Login required"
            DownloadErrorType.UNKNOWN -> "Something went wrong"
        }

    val body =
        message
            ?.takeUnless { it.isBlank() || it == errorType.name }
            ?: fallbackBody

    return title to body
}
