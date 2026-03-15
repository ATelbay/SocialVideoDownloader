package com.socialvideodownloader.feature.download.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.socialvideodownloader.core.ui.components.VideoInfoCard
import com.socialvideodownloader.core.ui.tokens.PlatformColors
import com.socialvideodownloader.feature.download.R
import com.socialvideodownloader.feature.download.ui.components.DownloadCompleteContent
import com.socialvideodownloader.feature.download.ui.components.DownloadErrorContent
import com.socialvideodownloader.feature.download.ui.components.DownloadProgressContent
import com.socialvideodownloader.feature.download.ui.components.FormatChipsContent
import com.socialvideodownloader.feature.download.ui.components.UrlInputContent

@Composable
fun DownloadScreen(
    viewModel: DownloadViewModel = hiltViewModel(),
    onToggleTheme: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is DownloadEvent.OpenFile -> {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(Uri.parse(event.filePath), "video/*")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, null))
                }
                is DownloadEvent.ShareFile -> {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "video/*"
                        putExtra(Intent.EXTRA_STREAM, Uri.parse(event.filePath))
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, null))
                }
            }
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.checkClipboard()
    }

    DownloadScreenContent(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        onToggleTheme = onToggleTheme,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DownloadScreenContent(
    uiState: DownloadUiState,
    onIntent: (DownloadIntent) -> Unit,
    onToggleTheme: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var urlText by rememberSaveable { mutableStateOf("") }
    val isIdle = uiState is DownloadUiState.Idle
    val clipboardUrl = (uiState as? DownloadUiState.Idle)?.clipboardUrl

    // Reset text field when returning to Idle (e.g., after "New Download")
    LaunchedEffect(isIdle, clipboardUrl) {
        if (isIdle) {
            urlText = clipboardUrl ?: ""
        }
    }

    val isDark = isSystemInDarkTheme()
    val iconColor by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(durationMillis = 300),
        label = "themeToggleIconColor",
    )

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_download)) },
                actions = {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    ) {
                        IconButton(onClick = onToggleTheme) {
                            Icon(
                                imageVector = if (isDark) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                                contentDescription = if (isDark) {
                                    stringResource(R.string.theme_toggle_light)
                                } else {
                                    stringResource(R.string.theme_toggle_dark)
                                },
                                tint = iconColor,
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            when (uiState) {
                is DownloadUiState.Idle -> {
                    UrlInputContent(
                        url = urlText,
                        onUrlChanged = { url ->
                            urlText = url
                            onIntent(DownloadIntent.UrlChanged(url))
                        },
                        onExtractClicked = { onIntent(DownloadIntent.ExtractClicked) },
                        isLoading = false,
                    )
                }

                is DownloadUiState.Extracting -> {
                    UrlInputContent(
                        url = uiState.url,
                        onUrlChanged = {},
                        onExtractClicked = {},
                        isLoading = true,
                    )
                }

                is DownloadUiState.FormatSelection -> {
                    VideoInfoCard(
                        thumbnailUrl = uiState.metadata.thumbnailUrl,
                        title = uiState.metadata.title,
                        uploaderName = uiState.metadata.author,
                        durationSeconds = uiState.metadata.durationSeconds,
                        platformName = platformFromUrl(uiState.metadata.sourceUrl),
                        platformColor = platformFromUrl(uiState.metadata.sourceUrl)
                            ?.let { PlatformColors.forPlatform(it) },
                        compact = false,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    FormatChipsContent(
                        formats = uiState.metadata.formats,
                        selectedFormatId = uiState.selectedFormatId,
                        onFormatSelected = { onIntent(DownloadIntent.FormatSelected(it)) },
                        onDownloadClicked = { onIntent(DownloadIntent.DownloadClicked) },
                    )
                }

                is DownloadUiState.Downloading -> {
                    VideoInfoCard(
                        thumbnailUrl = uiState.metadata.thumbnailUrl,
                        title = uiState.metadata.title,
                        uploaderName = uiState.metadata.author,
                        durationSeconds = uiState.metadata.durationSeconds,
                        platformName = platformFromUrl(uiState.metadata.sourceUrl),
                        platformColor = platformFromUrl(uiState.metadata.sourceUrl)
                            ?.let { PlatformColors.forPlatform(it) },
                        compact = false,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    DownloadProgressContent(
                        progress = uiState.progress,
                        onCancelClicked = { onIntent(DownloadIntent.CancelDownloadClicked) },
                    )
                }

                is DownloadUiState.Done -> {
                    DownloadCompleteContent(
                        metadata = uiState.metadata,
                        onOpenClicked = { onIntent(DownloadIntent.OpenFileClicked) },
                        onShareClicked = { onIntent(DownloadIntent.ShareFileClicked) },
                        onNewDownloadClicked = { onIntent(DownloadIntent.NewDownloadClicked) },
                    )
                }

                is DownloadUiState.Error -> {
                    DownloadErrorContent(
                        message = uiState.message,
                        onRetryClicked = { onIntent(DownloadIntent.RetryClicked) },
                        onNewDownloadClicked = { onIntent(DownloadIntent.NewDownloadClicked) },
                    )
                }
            }
        }
    }
}

private fun platformFromUrl(url: String): String? = when {
    url.contains("youtube.com", ignoreCase = true) || url.contains("youtu.be", ignoreCase = true) -> "YouTube"
    url.contains("instagram.com", ignoreCase = true) -> "Instagram"
    url.contains("tiktok.com", ignoreCase = true) -> "TikTok"
    url.contains("twitter.com", ignoreCase = true) || url.contains("x.com", ignoreCase = true) -> "Twitter"
    url.contains("vimeo.com", ignoreCase = true) -> "Vimeo"
    url.contains("facebook.com", ignoreCase = true) || url.contains("fb.watch", ignoreCase = true) -> "Facebook"
    else -> null
}
