package com.socialvideodownloader.shared.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.socialvideodownloader.shared.ui.theme.LocalAppShapes
import com.socialvideodownloader.shared.ui.theme.Spacing
import com.socialvideodownloader.shared.ui.theme.SvdBorder
import com.socialvideodownloader.shared.ui.theme.SvdForeground
import com.socialvideodownloader.shared.ui.theme.SvdPrimarySoft
import com.socialvideodownloader.shared.ui.theme.SvdPrimaryStrong
import com.socialvideodownloader.shared.ui.theme.SvdSurfaceAlt

@Composable
fun SvdTopBar(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    val shapes = LocalAppShapes.current
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = Spacing.TopBarHeight)
                .clip(shapes.control)
                .background(SvdSurfaceAlt)
                .border(1.dp, SvdBorder, shapes.control)
                .padding(horizontal = Spacing.TopBarPaddingH),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style =
                MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
            color = SvdForeground,
        )
        Spacer(Modifier.weight(1f))
        if (actionLabel != null) {
            Box(
                modifier =
                    Modifier
                        .clip(shapes.pill)
                        .background(SvdPrimarySoft)
                        .then(
                            if (onActionClick != null) {
                                Modifier
                                    .semantics(mergeDescendants = true) {
                                        role = Role.Button
                                        contentDescription = actionLabel
                                    }
                                    .clickable(onClick = onActionClick)
                            } else {
                                Modifier
                            },
                        )
                        .heightIn(min = Spacing.ActionChipHeight)
                        .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = actionLabel,
                    style =
                        MaterialTheme.typography.labelSmall.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    color = SvdPrimaryStrong,
                )
            }
        }
    }
}
