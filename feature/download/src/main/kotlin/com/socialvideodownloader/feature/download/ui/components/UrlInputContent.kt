package com.socialvideodownloader.feature.download.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.socialvideodownloader.core.ui.theme.Inter
import com.socialvideodownloader.core.ui.theme.SvdBorder
import com.socialvideodownloader.core.ui.theme.SvdPrimary
import com.socialvideodownloader.core.ui.theme.SvdSurface
import com.socialvideodownloader.core.ui.theme.SvdText
import com.socialvideodownloader.core.ui.theme.SvdTextTertiary
import com.socialvideodownloader.feature.download.R

@Composable
fun UrlInputContent(
    url: String,
    onUrlChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val clipboardManager = LocalClipboardManager.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SvdSurface)
            .border(1.dp, SvdBorder, RoundedCornerShape(16.dp))
            .padding(start = 16.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Default.Link,
            contentDescription = null,
            tint = SvdTextTertiary,
            modifier = Modifier.size(20.dp),
        )

        BasicTextField(
            value = url,
            onValueChange = onUrlChanged,
            singleLine = true,
            textStyle = TextStyle(
                fontFamily = Inter,
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp,
                color = SvdText,
            ),
            cursorBrush = SolidColor(SvdPrimary),
            modifier = Modifier.weight(1f),
            decorationBox = { innerTextField ->
                Box {
                    if (url.isEmpty()) {
                        Text(
                            text = stringResource(R.string.download_url_placeholder),
                            style = TextStyle(
                                fontFamily = Inter,
                                fontWeight = FontWeight.Normal,
                                fontSize = 15.sp,
                                color = SvdTextTertiary,
                            ),
                        )
                    }
                    innerTextField()
                }
            },
        )

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(SvdPrimary)
                .clickable {
                    clipboardManager.getText()?.text?.let { text ->
                        onUrlChanged(text)
                    }
                }
                .padding(vertical = 10.dp, horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = Icons.Default.ContentPaste,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = stringResource(R.string.download_paste_button),
                style = TextStyle(
                    fontFamily = Inter,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = Color.White,
                ),
            )
        }
    }
}
