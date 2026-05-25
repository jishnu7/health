package xyz.jishnu.health.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import xyz.jishnu.health.ui.theme.IntermTheme

@Composable
fun StageDots(
    count: Int,
    currentIdx: Int,
    modifier: Modifier = Modifier,
    lastWeight: Float = 1.5f,
) {
    val c = IntermTheme.colors
    Row(
        modifier = modifier.fillMaxWidth().height(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        for (i in 0 until count) {
            val filled = i <= currentIdx
            val color by animateColorAsState(if (filled) c.primary else c.border, label = "dot-$i")
            val isLast = i == count - 1
            Box(
                modifier = Modifier
                    .weight(if (isLast) lastWeight else 1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color),
            )
        }
    }
}
