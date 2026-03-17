package com.socialvideodownloader.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.socialvideodownloader.core.ui.theme.AppShapesInstance
import com.socialvideodownloader.core.ui.theme.SocialVideoDownloaderTheme
import com.socialvideodownloader.core.ui.theme.SvdBorder
import com.socialvideodownloader.core.ui.theme.SvdForeground
import com.socialvideodownloader.core.ui.theme.SvdPrimarySoft
import com.socialvideodownloader.core.ui.theme.SvdPrimaryStrong
import com.socialvideodownloader.core.ui.theme.SvdSurfaceAlt
import com.socialvideodownloader.core.ui.tokens.Spacing

@Composable
fun SvdTopBar(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(Spacing.TopBarHeight)
            .clip(AppShapesInstance.control)
            .background(SvdSurfaceAlt)
            .border(1.dp, SvdBorder, AppShapesInstance.control)
            .padding(horizontal = Spacing.TopBarPaddingH),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            color = SvdForeground,
        )
        Spacer(Modifier.weight(1f))
        if (actionLabel != null) {
            Box(
                modifier = Modifier
                    .clip(AppShapesInstance.pill)
                    .background(SvdPrimarySoft)
                    .then(
                        if (onActionClick != null) Modifier.clickable(onClick = onActionClick)
                        else Modifier,
                    )
                    .height(Spacing.ActionChipHeight)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = SvdPrimaryStrong,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SvdTopBarWithActionPreview() {
    SocialVideoDownloaderTheme {
        SvdTopBar(title = "New download", actionLabel = "Tips")
    }
}

@Preview(showBackground = true)
@Composable
private fun SvdTopBarNoActionPreview() {
    SocialVideoDownloaderTheme {
        SvdTopBar(title = "Downloading", actionLabel = "Hide", onActionClick = {})
    }
}
