package com.socialvideodownloader.shared.feature.download.ui

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.socialvideodownloader.shared.feature.download.DownloadEvent
import com.socialvideodownloader.shared.feature.download.DownloadIntent
import com.socialvideodownloader.shared.feature.download.DownloadUiState
import com.socialvideodownloader.shared.feature.download.SharedDownloadViewModel
import com.socialvideodownloader.shared.feature.download.platform.PlatformActions
import com.socialvideodownloader.shared.feature.download.platform.PlatformLoginScreen
import com.socialvideodownloader.shared.network.auth.CookieStore
import com.socialvideodownloader.shared.network.auth.SupportedPlatform
import com.socialvideodownloader.shared.ui.components.GradientButton
import com.socialvideodownloader.shared.ui.components.SecondaryButton
import com.socialvideodownloader.shared.ui.components.SvdTopBar
import com.socialvideodownloader.shared.ui.components.VideoInfoCard
import com.socialvideodownloader.shared.ui.theme.Spacing
import com.socialvideodownloader.shared.ui.theme.SvdBg
import com.socialvideodownloader.shared.ui.tokens.PlatformColors
import org.koin.mp.KoinPlatform

@Composable
fun DownloadScreen(
    viewModel: SharedDownloadViewModel,
    platformActions: PlatformActions,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showLoginForPlatform by remember { mutableStateOf<SupportedPlatform?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is DownloadEvent.OpenFile -> platformActions.openFile(event.filePath)
                is DownloadEvent.ShareFile -> platformActions.shareFile(event.filePath)
                is DownloadEvent.ShowSnackbarMessage -> snackbarHostState.showSnackbar(event.message)
                is DownloadEvent.ShowError -> snackbarHostState.showSnackbar(event.message ?: event.errorType.name)
                is DownloadEvent.RequestNotificationPermission -> platformActions.requestNotificationPermission()
                is DownloadEvent.ShowPlatformLogin -> {
                    showLoginForPlatform = event.platform
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

    showLoginForPlatform?.let { platform ->
        val secureCookieStore = remember { KoinPlatform.getKoin().get<CookieStore>() }
        PlatformLoginScreen(
            platform = platform,
            secureCookieStore = secureCookieStore,
            onResult = { success ->
                showLoginForPlatform = null
                viewModel.onIntent(DownloadIntent.PlatformLoginResult(platform, success))
            },
        )
    }
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
                is DownloadUiState.Idle ->
                    SvdTopBar(
                        title = "New download",
                        actionLabel = "Tips",
                    )
                is DownloadUiState.Extracting ->
                    SvdTopBar(title = "New download")
                is DownloadUiState.FormatSelection ->
                    SvdTopBar(
                        title = "Select format",
                        actionLabel = "Back",
                        onActionClick = { onIntent(DownloadIntent.BackToIdleClicked) },
                    )
                is DownloadUiState.Downloading ->
                    SvdTopBar(
                        title = "Downloading",
                        actionLabel = "Hide",
                        onActionClick = { onIntent(DownloadIntent.NewDownloadClicked) },
                    )
                is DownloadUiState.Done ->
                    SvdTopBar(title = "Complete")
                is DownloadUiState.Error ->
                    SvdTopBar(title = "Error")
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
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState()),
            label = "downloadStateTransition",
        ) { targetState ->
            val nonIdlePadding =
                Modifier.padding(
                    top = 8.dp,
                    start = Spacing.ScreenPadding,
                    end = Spacing.ScreenPadding,
                    bottom = Spacing.ScreenPadding,
                )
            when (targetState) {
                is DownloadUiState.Idle ->
                    IdleContent(
                        url = urlText,
                        existingDownload = targetState.existingDownload,
                        connectedPlatforms = targetState.connectedPlatforms,
                        onDisconnect = { platform ->
                            onIntent(DownloadIntent.DisconnectPlatformClicked(platform))
                        },
                        onUrlChanged = { url ->
                            urlText = url
                            onIntent(DownloadIntent.UrlChanged(url))
                        },
                        onExtractClicked = { onIntent(DownloadIntent.ExtractClicked) },
                        onOpenExistingClicked = { onIntent(DownloadIntent.OpenExistingClicked) },
                        onShareExistingClicked = { onIntent(DownloadIntent.ShareExistingClicked) },
                        onDismissExistingBanner = { onIntent(DownloadIntent.DismissExistingBanner) },
                    )

                is DownloadUiState.Extracting ->
                    ExtractingContent(
                        url = targetState.url,
                        onCancelClicked = { onIntent(DownloadIntent.NewDownloadClicked) },
                        modifier = nonIdlePadding,
                    )

                is DownloadUiState.FormatSelection ->
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
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            GradientButton(
                                text = "Download",
                                onClick = { onIntent(DownloadIntent.DownloadClicked) },
                                modifier = Modifier.weight(1f),
                            )
                            SecondaryButton(
                                text = "Share",
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

                is DownloadUiState.Downloading ->
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

                is DownloadUiState.Done ->
                    DownloadCompleteContent(
                        metadata = targetState.metadata,
                        onOpenClicked = { onIntent(DownloadIntent.OpenFileClicked) },
                        onShareClicked = { onIntent(DownloadIntent.ShareFileClicked) },
                        onNewDownloadClicked = { onIntent(DownloadIntent.NewDownloadClicked) },
                        modifier = nonIdlePadding,
                    )

                is DownloadUiState.Error ->
                    DownloadErrorContent(
                        errorType = targetState.errorType,
                        message = targetState.message,
                        platformForAuth = targetState.platformForAuth,
                        isReconnect = targetState.isReconnect,
                        onConnectPlatformClicked = { platform ->
                            onIntent(DownloadIntent.ConnectPlatformClicked(platform))
                        },
                        onRetryClicked = { onIntent(DownloadIntent.RetryClicked) },
                        onNewDownloadClicked = { onIntent(DownloadIntent.NewDownloadClicked) },
                        modifier = nonIdlePadding,
                    )
            }
        }
    }
}
