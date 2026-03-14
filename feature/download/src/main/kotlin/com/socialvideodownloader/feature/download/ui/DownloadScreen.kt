package com.socialvideodownloader.feature.download.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.socialvideodownloader.feature.download.R
import com.socialvideodownloader.feature.download.ui.components.DownloadCompleteContent
import com.socialvideodownloader.feature.download.ui.components.DownloadErrorContent
import com.socialvideodownloader.feature.download.ui.components.DownloadProgressContent
import com.socialvideodownloader.feature.download.ui.components.FormatChipsContent
import com.socialvideodownloader.feature.download.ui.components.UrlInputContent
import com.socialvideodownloader.feature.download.ui.components.VideoInfoContent

@Composable
fun DownloadScreen(
    viewModel: DownloadViewModel = hiltViewModel(),
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
        modifier = modifier,
    )
}

@Composable
private fun DownloadScreenContent(
    uiState: DownloadUiState,
    onIntent: (DownloadIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    var urlText by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        when (uiState) {
            is DownloadUiState.Idle -> {
                LaunchedEffect(uiState.clipboardUrl) {
                    uiState.clipboardUrl?.let { urlText = it }
                }
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
                VideoInfoContent(metadata = uiState.metadata)
                Spacer(modifier = Modifier.height(16.dp))
                FormatChipsContent(
                    formats = uiState.metadata.formats,
                    selectedFormatId = uiState.selectedFormatId,
                    onFormatSelected = { onIntent(DownloadIntent.FormatSelected(it)) },
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { onIntent(DownloadIntent.DownloadClicked) },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Text(text = stringResource(R.string.download_button))
                }
            }

            is DownloadUiState.Downloading -> {
                VideoInfoContent(metadata = uiState.metadata)
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
