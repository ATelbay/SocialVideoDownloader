package com.socialvideodownloader.feature.download.ui

import android.content.Context
import com.socialvideodownloader.feature.download.R
import com.socialvideodownloader.shared.data.platform.DownloadErrorType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Localizes a [DownloadErrorType] into user-facing notification text.
 *
 * Classification (raw [Throwable] → [DownloadErrorType]) is owned by the shared
 * `DownloadErrorClassifier`, so this mapper is a pure enum → string resolver and
 * cannot drift from the in-app error UI, which resolves the same enum.
 */
class ErrorMessageMapper
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        fun map(errorType: DownloadErrorType): String =
            when (errorType) {
                DownloadErrorType.UNSUPPORTED_URL ->
                    context.getString(R.string.download_error_unsupported_url)
                DownloadErrorType.NETWORK_ERROR ->
                    context.getString(R.string.download_error_network)
                DownloadErrorType.STORAGE_FULL ->
                    context.getString(R.string.download_error_storage)
                DownloadErrorType.EXTRACTION_FAILED ->
                    context.getString(R.string.download_error_unavailable)
                DownloadErrorType.COPYRIGHT ->
                    context.getString(R.string.download_error_copyright)
                DownloadErrorType.SERVER_UNAVAILABLE,
                DownloadErrorType.AUTH_REQUIRED,
                DownloadErrorType.DOWNLOAD_FAILED,
                DownloadErrorType.UNKNOWN,
                ->
                    context.getString(R.string.download_error_generic)
            }
    }
