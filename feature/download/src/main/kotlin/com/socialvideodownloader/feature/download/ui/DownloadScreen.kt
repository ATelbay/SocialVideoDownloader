package com.socialvideodownloader.feature.download.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.socialvideodownloader.feature.download.ui.components.ExtractingContent
import com.socialvideodownloader.feature.download.ui.components.FormatChipsContent
import com.socialvideodownloader.feature.download.ui.components.IdleContent

@Composable
fun DownloadScreen(
    isDarkTheme: Boolean,
    viewModel: DownloadViewModel = hiltViewModel(),
    onToggleTheme: () -> Unit = {},
    initialUrl: String? = null,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(initialUrl) {
        if (initialUrl != null) {
            viewModel.onIntent(DownloadIntent.PrefillUrl(initialUrl))
        }
    }

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
        isDarkTheme = isDarkTheme,
        onToggleTheme = onToggleTheme,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DownloadScreenContent(
    uiState: DownloadUiState,
    onIntent: (DownloadIntent) -> Unit,
    isDarkTheme: Boolean,
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

    val isDark = isDarkTheme
    val iconColor by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(durationMillis = 300),
        label = "themeToggleIconColor",
    )

    val titleResId = when (uiState) {
        is DownloadUiState.Idle, is DownloadUiState.Extracting -> R.string.download_screen_title
        is DownloadUiState.FormatSelection -> R.string.download_title_select_format
        is DownloadUiState.Downloading -> R.string.download_title_downloading
        is DownloadUiState.Done -> R.string.download_title_complete
        is DownloadUiState.Error -> R.string.download_title_error
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(stringResource(titleResId)) },
                    navigationIcon = {
                        if (!isIdle) {
                            IconButton(onClick = { onIntent(DownloadIntent.NewDownloadClicked) }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = null,
                                )
                            }
                        }
                    },
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
                                    contentDescription = if (isDark) "Switch to light mode" else "Switch to dark mode",
                                    tint = iconColor,
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
            }
        },
    ) { innerPadding ->
        AnimatedContent(
            targetState = uiState,
            contentKey = { it::class },
            transitionSpec = {
                (fadeIn(tween(300)) + scaleIn(initialScale = 0.92f, animationSpec = tween(300)))
                    .togetherWith(fadeOut(tween(200)))
                    .using(SizeTransform(clip = false))
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            label = "downloadStateTransition",
        ) { targetState ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                when (targetState) {
                    is DownloadUiState.Idle -> {
                        IdleContent(
                            url = urlText,
                            onUrlChanged = { url ->
                                urlText = url
                                onIntent(DownloadIntent.UrlChanged(url))
                            },
                            onExtractClicked = { onIntent(DownloadIntent.ExtractClicked) },
                        )
                    }

                    is DownloadUiState.Extracting -> {
                        ExtractingContent(
                            url = targetState.url,
                            onCancelClicked = { onIntent(DownloadIntent.NewDownloadClicked) },
                        )
                    }

                    is DownloadUiState.FormatSelection -> {
                        VideoInfoCard(
                            thumbnailUrl = targetState.metadata.thumbnailUrl,
                            title = targetState.metadata.title,
                            uploaderName = targetState.metadata.author,
                            durationSeconds = targetState.metadata.durationSeconds,
                            platformName = PlatformColors.nameFromUrl(targetState.metadata.sourceUrl),
                            platformColor = PlatformColors.nameFromUrl(targetState.metadata.sourceUrl)
                                ?.let { PlatformColors.forPlatform(it) },
                            compact = false,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        FormatChipsContent(
                            formats = targetState.metadata.formats,
                            selectedFormatId = targetState.selectedFormatId,
                            onFormatSelected = { onIntent(DownloadIntent.FormatSelected(it)) },
                            onDownloadClicked = { onIntent(DownloadIntent.DownloadClicked) },
                        )
                    }

                    is DownloadUiState.Downloading -> {
                        VideoInfoCard(
                            thumbnailUrl = targetState.metadata.thumbnailUrl,
                            title = targetState.metadata.title,
                            uploaderName = targetState.metadata.author,
                            durationSeconds = targetState.metadata.durationSeconds,
                            platformName = PlatformColors.nameFromUrl(targetState.metadata.sourceUrl),
                            platformColor = PlatformColors.nameFromUrl(targetState.metadata.sourceUrl)
                                ?.let { PlatformColors.forPlatform(it) },
                            compact = false,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        DownloadProgressContent(
                            progress = targetState.progress,
                            onCancelClicked = { onIntent(DownloadIntent.CancelDownloadClicked) },
                        )
                    }

                    is DownloadUiState.Done -> {
                        DownloadCompleteContent(
                            metadata = targetState.metadata,
                            onOpenClicked = { onIntent(DownloadIntent.OpenFileClicked) },
                            onShareClicked = { onIntent(DownloadIntent.ShareFileClicked) },
                            onNewDownloadClicked = { onIntent(DownloadIntent.NewDownloadClicked) },
                        )
                    }

                    is DownloadUiState.Error -> {
                        DownloadErrorContent(
                            message = targetState.message,
                            onRetryClicked = { onIntent(DownloadIntent.RetryClicked) },
                            onNewDownloadClicked = { onIntent(DownloadIntent.NewDownloadClicked) },
                        )
                    }
                }
            }
        }
    }
}

