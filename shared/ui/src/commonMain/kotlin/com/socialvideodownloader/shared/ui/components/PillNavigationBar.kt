package com.socialvideodownloader.shared.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.socialvideodownloader.shared.ui.theme.LocalAppShapes
import com.socialvideodownloader.shared.ui.theme.Spacing
import com.socialvideodownloader.shared.ui.theme.SvdBg
import com.socialvideodownloader.shared.ui.theme.SvdBorder
import com.socialvideodownloader.shared.ui.theme.SvdPrimarySoft
import com.socialvideodownloader.shared.ui.theme.SvdPrimaryStrong
import com.socialvideodownloader.shared.ui.theme.SvdSubtleForeground
import com.socialvideodownloader.shared.ui.theme.SvdSurface

/**
 * A single tab in [PillNavigationBar]. The [label] is supplied by the caller so it can be
 * localized via platform string resources — the design system stays string-free.
 */
@Immutable
data class PillNavItem(
    val label: String,
    val icon: ImageVector,
)

/**
 * Canonical icons for the app's primary navigation destinations. Centralized here so the
 * Android and iOS shells share one icon set instead of choosing (and potentially diverging on)
 * their own.
 */
object SvdNavIcons {
    val Download: ImageVector = Icons.Outlined.Download
    val Library: ImageVector = Icons.Outlined.FolderOpen
    val History: ImageVector = Icons.Outlined.History
}

@Composable
fun PillNavigationBar(
    items: List<PillNavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shapes = LocalAppShapes.current

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .background(SvdBg),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        top = Spacing.NavBarPaddingTop,
                        start = Spacing.NavBarPaddingH,
                        end = Spacing.NavBarPaddingH,
                        bottom = Spacing.NavBarPaddingBottom,
                    )
                    .heightIn(min = Spacing.NavBarHeight)
                    .clip(shapes.pill)
                    .background(SvdSurface)
                    .border(width = 1.dp, color = SvdBorder, shape = shapes.pill)
                    .padding(Spacing.NavBarInternalPadding),
        ) {
            items.forEachIndexed { index, tab ->
                val isSelected = index == selectedIndex
                Column(
                    modifier =
                        Modifier
                            .weight(1f)
                            .heightIn(min = Spacing.NavTabMinHeight)
                            .clip(shapes.navTab)
                            .background(if (isSelected) SvdPrimarySoft else Color.Transparent)
                            .semantics(mergeDescendants = true) {
                                role = Role.Tab
                                selected = isSelected
                            }
                            .clickable { onSelect(index) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.GapXs, Alignment.CenterVertically),
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = null,
                        tint = if (isSelected) SvdPrimaryStrong else SvdSubtleForeground,
                        modifier = Modifier.size(Spacing.IconMd),
                    )
                    Text(
                        text = tab.label,
                        color = if (isSelected) SvdPrimaryStrong else SvdSubtleForeground,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        letterSpacing = 0.5.sp,
                    )
                }
            }
        }
    }
}
