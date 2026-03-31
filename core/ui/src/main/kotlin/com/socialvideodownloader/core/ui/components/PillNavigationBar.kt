package com.socialvideodownloader.core.ui.components

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.socialvideodownloader.core.ui.R
import com.socialvideodownloader.core.ui.theme.AppShapesInstance
import com.socialvideodownloader.core.ui.theme.SvdBorder
import com.socialvideodownloader.core.ui.theme.SvdPrimarySoft
import com.socialvideodownloader.core.ui.theme.SvdPrimaryStrong
import com.socialvideodownloader.core.ui.theme.SvdSubtleForeground
import com.socialvideodownloader.core.ui.theme.SvdSurface
import com.socialvideodownloader.core.ui.tokens.Spacing

@Composable
fun PillNavigationBar(
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabs =
        listOf(
            TabData(label = stringResource(R.string.nav_tab_download), icon = Icons.Outlined.Download),
            TabData(label = stringResource(R.string.nav_tab_library), icon = Icons.Outlined.FolderOpen),
            TabData(label = stringResource(R.string.nav_tab_history), icon = Icons.Outlined.History),
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
                .clip(AppShapesInstance.pill)
                .background(SvdSurface)
                .border(width = 1.dp, color = SvdBorder, shape = AppShapesInstance.pill)
                .padding(Spacing.NavBarInternalPadding),
    ) {
        tabs.forEachIndexed { index, tab ->
            val isSelected = index == selectedIndex
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .height(54.dp)
                        .clip(AppShapesInstance.navTab)
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
