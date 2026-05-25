package xyz.jishnu.health.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.jishnu.health.ui.theme.IntermTheme

enum class NavTab(val label: String) {
    Today("Today"),
    Weight("Weight"),
    Progress("Progress"),
}

@Composable
fun BottomNav(
    active: NavTab,
    onChange: (NavTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = IntermTheme.colors
    Column(modifier = modifier.fillMaxWidth().background(c.surface)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.border))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            NavTab.entries.forEach { tab ->
                NavItem(
                    label = tab.label,
                    icon = iconFor(tab),
                    active = tab == active,
                    onClick = { onChange(tab) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

private fun iconFor(tab: NavTab): ImageVector = when (tab) {
    NavTab.Today -> IntermIcons.Home
    NavTab.Weight -> IntermIcons.Scale
    NavTab.Progress -> IntermIcons.Chart
}

@Composable
private fun NavItem(
    label: String,
    icon: ImageVector,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = IntermTheme.colors
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(width = 48.dp, height = 28.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (active) c.primarySoft else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = if (active) c.primary else c.muted)
        }
        Text(
            text = label,
            color = if (active) c.ink else c.muted,
            style = IntermTheme.typography.caption.copy(fontSize = 10.sp),
        )
    }
}
