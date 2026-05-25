package xyz.jishnu.health.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.jishnu.health.ui.theme.IntermTheme

@Composable
fun IntermStageChip(
    label: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val c = IntermTheme.colors
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(c.primarySoft)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(c.primary),
        )
        Text(
            text = label,
            color = c.primary,
            style = IntermTheme.typography.body.copy(fontSize = 12.sp),
        )
    }
}
