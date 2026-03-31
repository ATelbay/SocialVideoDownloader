package com.socialvideodownloader.shared.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
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
import com.socialvideodownloader.shared.ui.theme.SvdSurface

@Composable
fun FormatChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shapes = LocalAppShapes.current
    val backgroundColor = if (selected) SvdPrimarySoft else SvdSurface
    val border = if (selected) null else BorderStroke(1.dp, SvdBorder)
    val labelColor = if (selected) SvdPrimaryStrong else SvdForeground
    val fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
    val isSelected = selected

    Surface(
        onClick = onClick,
        shape = shapes.pill,
        color = backgroundColor,
        border = border,
        modifier =
            modifier.semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = label
                this.selected = isSelected
            },
    ) {
        Text(
            text = label,
            style =
                MaterialTheme.typography.labelMedium.copy(
                    fontSize = 13.sp,
                    fontWeight = fontWeight,
                    color = labelColor,
                ),
            modifier =
                Modifier.padding(
                    horizontal = Spacing.ChipPaddingH,
                    vertical = Spacing.ChipPaddingV,
                ),
        )
    }
}
