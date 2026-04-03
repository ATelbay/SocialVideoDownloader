package com.socialvideodownloader.shared.feature.library.platform

import androidx.compose.runtime.Composable

@Composable
expect fun formatFileSize(bytes: Long): String

@Composable
expect fun formatRelativeTime(epochMillis: Long): String
