package com.socialvideodownloader.shared.feature.download.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.socialvideodownloader.shared.network.auth.SupportedPlatform
import com.socialvideodownloader.shared.ui.components.SecondaryButton
import com.socialvideodownloader.shared.ui.theme.SvdForeground
import com.socialvideodownloader.shared.ui.theme.SvdMutedForeground

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PlatformConnectionChips(
    connectedPlatforms: List<SupportedPlatform>,
    onDisconnect: (SupportedPlatform) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (connectedPlatforms.isEmpty()) return

    var selectedPlatform by remember { mutableStateOf<SupportedPlatform?>(null) }

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        connectedPlatforms.forEach { platform ->
            AssistChip(
                onClick = { selectedPlatform = platform },
                label = { Text(platform.displayName) },
            )
        }
    }

    selectedPlatform?.let { platform ->
        ModalBottomSheet(
            onDismissRequest = { selectedPlatform = null },
            sheetState = rememberModalBottomSheetState(),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = platform.displayName,
                    style = MaterialTheme.typography.titleLarge,
                    color = SvdForeground,
                )
                Text(
                    text = "Connected",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SvdMutedForeground,
                )
                SecondaryButton(
                    text = DownloadAuthStrings.disconnectLabel,
                    onClick = {
                        onDisconnect(platform)
                        selectedPlatform = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
