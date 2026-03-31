package com.socialvideodownloader.shared.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.socialvideodownloader.shared.ui.theme.LocalAppShapes
import com.socialvideodownloader.shared.ui.theme.SvdBorder
import com.socialvideodownloader.shared.ui.theme.SvdPrimarySoft
import com.socialvideodownloader.shared.ui.theme.SvdPrimaryStrong
import com.socialvideodownloader.shared.ui.theme.SvdSubtleForeground
import com.socialvideodownloader.shared.ui.theme.SvdSurface
import com.socialvideodownloader.shared.ui.tokens.Spacing

@Composable
fun PillNavigationBar(
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shapes = LocalAppShapes.current
    val tabs =
        listOf(
            TabData(label = "Download", icon = Icons.Outlined.Download),
            TabData(label = "Library", icon = Icons.Outlined.FolderOpen),
            TabData(label = "History", icon = Icons.Outlined.History),
        )

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(
                    top = Spacing.NavBarPaddingTop,
                    start = Spacing.NavBarPaddingH,
                    end = Spacing.NavBarPaddingH,
                    bottom = Spacing.NavBarPaddingBottom,
                )
                .height(Spacing.NavBarHeight)
                .clip(shapes.pill)
                .background(SvdSurface)
                .border(width = 1.dp, color = SvdBorder, shape = shapes.pill)
                .padding(Spacing.NavBarInternalPadding),
    ) {
        tabs.forEachIndexed { index, tab ->
            val isSelected = index == selectedIndex
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .height(54.dp)
                        .clip(shapes.navTab)
                        .background(if (isSelected) SvdPrimarySoft else Color.Transparent)
                        .clickable { onSelect(index) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
            ) {
                Icon(
                    imageVector = tab.icon,
                    contentDescription = tab.label,
                    tint = if (isSelected) SvdPrimaryStrong else SvdSubtleForeground,
                    modifier = Modifier.size(18.dp),
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

private data class TabData(val label: String, val icon: ImageVector)
