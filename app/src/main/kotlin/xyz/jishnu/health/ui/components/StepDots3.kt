package xyz.jishnu.health.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import xyz.jishnu.health.ui.theme.IntermTheme

@Composable
fun StepDots3(
    total: Int = 3,
    currentStep: Int,
    modifier: Modifier = Modifier,
    activeColor: Color? = null,
) {
    val c = IntermTheme.colors
    val onColor = activeColor ?: c.primary
    Row(
        modifier = modifier.fillMaxWidth().height(4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        for (i in 0 until total) {
            val filled = i < currentStep
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (filled) onColor else c.border),
            )
        }
    }
}
