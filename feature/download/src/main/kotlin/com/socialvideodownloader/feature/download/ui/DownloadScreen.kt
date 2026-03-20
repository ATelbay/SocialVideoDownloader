package com.socialvideodownloader.feature.download.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.socialvideodownloader.core.ui.components.GradientButton
import com.socialvideodownloader.core.ui.components.SecondaryButton
import com.socialvideodownloader.core.ui.components.SvdTopBar
import com.socialvideodownloader.core.ui.components.VideoInfoCard
import com.socialvideodownloader.core.ui.theme.SvdBg
import com.socialvideodownloader.core.ui.tokens.PlatformColors
import com.socialvideodownloader.core.ui.tokens.Spacing
import com.socialvideodownloader.feature.download.R
import com.socialvideodownloader.feature.download.ui.components.DownloadCompleteContent
import com.socialvideodownloader.feature.download.ui.components.DownloadErrorContent
import com.socialvideodownloader.feature.download.ui.components.DownloadProgressContent
import com.socialvideodownloader.feature.download.ui.components.ExtractingContent
import com.socialvideodownloader.feature.download.ui.components.FormatChipsContent
import com.socialvideodownloader.feature.download.ui.components.IdleContent

@Composable
fun DownloadScreen(
    viewModel: DownloadViewModel = hiltViewModel(),
    initialUrl: String? = null,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.onNotificationPermissionResult(granted)
    }

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
                is DownloadEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is DownloadEvent.RequestNotificationPermission -> {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    DownloadScreenContent(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

@Composable
private fun DownloadScreenContent(
    uiState: DownloadUiState,
    onIntent: (DownloadIntent) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    var urlText by rememberSaveable { mutableStateOf("") }

    val prefillUrl = (uiState as? DownloadUiState.Idle)?.prefillUrl
    LaunchedEffect(prefillUrl) {
        if (prefillUrl != null) {
            urlText = prefillUrl
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = SvdBg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            when (uiState) {
                is DownloadUiState.Idle -> {
                    SvdTopBar(
                        title = stringResource(R.string.download_screen_title),
                        actionLabel = stringResource(R.string.download_action_tips),
                    )
                }
                is DownloadUiState.Extracting -> {
                    SvdTopBar(
                        title = stringResource(R.string.download_screen_title),
                    )
                }
                is DownloadUiState.FormatSelection -> {
                    SvdTopBar(
                        title = stringResource(R.string.download_title_select_format),
                        actionLabel = stringResource(R.string.download_action_back),
                        onActionClick = { onIntent(DownloadIntent.NewDownloadClicked) },
                    )
                }
                is DownloadUiState.Downloading -> {
                    SvdTopBar(
                        title = stringResource(R.string.download_title_downloading),
                        actionLabel = stringResource(R.string.download_action_hide),
                        onActionClick = { onIntent(DownloadIntent.NewDownloadClicked) },
                    )
                }
                is DownloadUiState.Done -> {
                    SvdTopBar(
                        title = stringResource(R.string.download_title_complete),
                    )
                }
                is DownloadUiState.Error -> {
                    SvdTopBar(
                        title = stringResource(R.string.download_title_error),
                    )
                }
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
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            label = "downloadStateTransition",
        ) { targetState ->
            val nonIdlePadding = Modifier.padding(
                top = 8.dp,
                start = Spacing.ScreenPadding,
                end = Spacing.ScreenPadding,
                bottom = Spacing.ScreenPadding,
            )
            when (targetState) {
                is DownloadUiState.Idle -> {
                    IdleContent(
                        url = urlText,
                        existingDownload = targetState.existingDownload,
                        onUrlChanged = { url ->
                            urlText = url
                            onIntent(DownloadIntent.UrlChanged(url))
                        },
                        onExtractClicked = { onIntent(DownloadIntent.ExtractClicked) },
                        onOpenExistingClicked = { onIntent(DownloadIntent.OpenExistingClicked) },
                        onShareExistingClicked = { onIntent(DownloadIntent.ShareExistingClicked) },
                        onDismissExistingBanner = { onIntent(DownloadIntent.DismissExistingBanner) },
                    )
                }

                is DownloadUiState.Extracting -> {
                    ExtractingContent(
                        url = targetState.url,
                        onCancelClicked = { onIntent(DownloadIntent.NewDownloadClicked) },
                        modifier = nonIdlePadding,
                    )
                }

                is DownloadUiState.FormatSelection -> {
                    Column(modifier = nonIdlePadding) {
                        VideoInfoCard(
                            thumbnailUrl = targetState.metadata.thumbnailUrl,
                            title = targetState.metadata.title,
                            uploaderName = targetState.metadata.author,
                            durationSeconds = targetState.metadata.durationSeconds,
                            platformName = PlatformColors.nameFromUrl(targetState.metadata.sourceUrl),
                            compact = false,
                        )
                        Spacer(modifier = Modifier.height(Spacing.SectionGap))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            GradientButton(
                                text = stringResource(R.string.download_button),
                                onClick = { onIntent(DownloadIntent.DownloadClicked) },
                                modifier = Modifier.weight(1f),
                            )
                            SecondaryButton(
                                text = stringResource(R.string.download_share),
                                onClick = { onIntent(DownloadIntent.ShareFormatClicked) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Spacer(modifier = Modifier.height(Spacing.SectionGap))
                        FormatChipsContent(
                            formats = targetState.metadata.formats,
                            selectedFormatId = targetState.selectedFormatId,
                            onFormatSelected = { onIntent(DownloadIntent.FormatSelected(it)) },
                        )
                    }
                }

                is DownloadUiState.Downloading -> {
                    Column(modifier = nonIdlePadding) {
                        VideoInfoCard(
                            thumbnailUrl = targetState.metadata.thumbnailUrl,
                            title = targetState.metadata.title,
                            uploaderName = targetState.metadata.author,
                            durationSeconds = targetState.metadata.durationSeconds,
                            platformName = PlatformColors.nameFromUrl(targetState.metadata.sourceUrl),
                            compact = true,
                        )
                        Spacer(modifier = Modifier.height(Spacing.SectionGap))
                        DownloadProgressContent(
                            progress = targetState.progress,
                            onCancelClicked = { onIntent(DownloadIntent.CancelDownloadClicked) },
                        )
                    }
                }

                is DownloadUiState.Done -> {
                    DownloadCompleteContent(
                        metadata = targetState.metadata,
                        onOpenClicked = { onIntent(DownloadIntent.OpenFileClicked) },
                        onShareClicked = { onIntent(DownloadIntent.ShareFileClicked) },
                        onNewDownloadClicked = { onIntent(DownloadIntent.NewDownloadClicked) },
                        modifier = nonIdlePadding,
                    )
                }

                is DownloadUiState.Error -> {
                    DownloadErrorContent(
                        message = targetState.message,
                        onRetryClicked = { onIntent(DownloadIntent.RetryClicked) },
                        onNewDownloadClicked = { onIntent(DownloadIntent.NewDownloadClicked) },
                        modifier = nonIdlePadding,
                    )
                }
            }
        }
    }
}
