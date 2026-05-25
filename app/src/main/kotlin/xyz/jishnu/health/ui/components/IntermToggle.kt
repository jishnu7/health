package xyz.jishnu.health.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import xyz.jishnu.health.ui.theme.IntermTheme

@Composable
fun IntermToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val c = IntermTheme.colors
    val track by animateColorAsState(if (checked) c.primary else c.border, tween(150), label = "toggle-track")
    val offsetX by animateDpAsState(if (checked) 21.dp else 3.dp, tween(180), label = "toggle-thumb")

    Box(
        modifier = modifier
            .size(width = 44.dp, height = 26.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(track)
            .clickable(
                enabled = enabled,
                role = Role.Switch,
                onClick = { onCheckedChange(!checked) },
            ),
    ) {
        Box(
            modifier = Modifier
                .offset(x = offsetX, y = 3.dp)
                .size(20.dp)
                .shadow(elevation = 2.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(c.surface),
        )
    }
}
