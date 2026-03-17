package com.socialvideodownloader.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.socialvideodownloader.R
import com.socialvideodownloader.core.ui.theme.SvdSubtleForeground
import kotlinx.serialization.Serializable

@Serializable
data object LibraryRoute

@Composable
fun LibraryScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.library_coming_soon),
            style = MaterialTheme.typography.bodyMedium,
            color = SvdSubtleForeground,
        )
    }
}
