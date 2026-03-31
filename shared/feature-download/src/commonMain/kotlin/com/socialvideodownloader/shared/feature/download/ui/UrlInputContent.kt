package com.socialvideodownloader.shared.feature.download.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.socialvideodownloader.shared.ui.theme.LocalAppShapes
import com.socialvideodownloader.shared.ui.theme.SvdBorder
import com.socialvideodownloader.shared.ui.theme.SvdForeground
import com.socialvideodownloader.shared.ui.theme.SvdPrimary
import com.socialvideodownloader.shared.ui.theme.SvdSubtleForeground
import com.socialvideodownloader.shared.ui.theme.SvdSurface
import com.socialvideodownloader.shared.ui.theme.SvdSurfaceAlt
import com.socialvideodownloader.shared.ui.theme.Spacing

@Composable
fun UrlInputContent(
    url: String,
    onUrlChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shapes = LocalAppShapes.current
    val clipboardManager = LocalClipboardManager.current

    val urlTextStyle =
        TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp,
            color = SvdForeground,
        )

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(Spacing.InputHeight)
                .clip(shapes.control)
                .background(SvdSurface)
                .border(1.dp, SvdBorder, shapes.control)
                .padding(start = 16.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BasicTextField(
            value = url,
            onValueChange = onUrlChanged,
            singleLine = true,
            textStyle = urlTextStyle,
            cursorBrush = SolidColor(SvdPrimary),
            modifier = Modifier.weight(1f),
            decorationBox = { innerTextField ->
                Box {
                    if (url.isEmpty()) {
                        Text(
                            text = "Paste a video link",
                            style =
                                urlTextStyle.copy(
                                    color = SvdSubtleForeground,
                                    fontSize = 15.sp,
                                ),
                        )
                    }
                    innerTextField()
                }
            },
        )

        Box(
            modifier =
                Modifier
                    .clip(shapes.pill)
                    .background(SvdSurfaceAlt)
                    .clickable {
                        clipboardManager.getText()?.text?.let { text ->
                            if (text.isNotBlank()) onUrlChanged(text)
                        }
                    }
                    .height(32.dp)
                    .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Paste",
                style =
                    MaterialTheme.typography.labelSmall.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                color = SvdForeground,
            )
        }
    }
}
