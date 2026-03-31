package com.socialvideodownloader.feature.download.ui

import android.content.Context
import com.socialvideodownloader.feature.download.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class ErrorMessageMapper
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        fun map(exception: Throwable): String {
            val message = exception.message ?: return context.getString(R.string.download_error_generic)
            return when {
                message.contains("Unsupported URL", ignoreCase = true) ->
                    context.getString(R.string.download_error_unsupported_url)
                message.contains("unavailable", ignoreCase = true) ||
                    message.contains("private", ignoreCase = true) ->
                    context.getString(R.string.download_error_unavailable)
                message.contains("network", ignoreCase = true) ||
                    message.contains("connect", ignoreCase = true) ||
                    message.contains("internet", ignoreCase = true) ->
                    context.getString(R.string.download_error_network)
                message.contains("space", ignoreCase = true) ||
                    message.contains("storage", ignoreCase = true) ->
                    context.getString(R.string.download_error_storage)
                else -> context.getString(R.string.download_error_generic)
            }
        }
    }
