package com.socialvideodownloader.shared.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.socialvideodownloader.shared.ui.theme.LocalAppShapes
import com.socialvideodownloader.shared.ui.theme.SvdBorder
import com.socialvideodownloader.shared.ui.theme.SvdForeground
import com.socialvideodownloader.shared.ui.theme.Spacing

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val shapes = LocalAppShapes.current
    Box(
        modifier =
            modifier
                .height(Spacing.PrimaryButtonHeight)
                .clip(shapes.control)
                .alpha(if (enabled) 1f else 0.5f)
                .border(1.dp, SvdBorder, shapes.control)
                .semantics {
                    role = Role.Button
                    contentDescription = text
                }
                .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style =
                MaterialTheme.typography.labelLarge.copy(
                    letterSpacing = 0.6.sp,
                    color = SvdForeground,
                ),
        )
    }
}
