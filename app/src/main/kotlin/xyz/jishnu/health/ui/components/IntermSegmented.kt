package xyz.jishnu.health.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.jishnu.health.ui.theme.IntermTheme

data class SegmentedOption<T>(val value: T, val label: String)

@Composable
fun <T> IntermSegmented(
    options: List<SegmentedOption<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = IntermTheme.colors
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(c.border2)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEach { opt ->
            val active = opt.value == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .let {
                        if (active) {
                            it
                                .shadow(elevation = 1.dp, shape = RoundedCornerShape(999.dp))
                                .background(c.surface)
                        } else {
                            it.background(Color.Transparent)
                        }
                    }
                    .clickable { onSelect(opt.value) }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = opt.label,
                    style = IntermTheme.typography.body.copy(fontSize = 13.sp),
                    color = if (active) c.ink else c.muted,
                )
            }
        }
    }
}
