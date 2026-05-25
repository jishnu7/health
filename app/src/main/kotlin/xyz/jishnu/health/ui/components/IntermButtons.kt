package xyz.jishnu.health.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import xyz.jishnu.health.ui.theme.IntermTheme

enum class IntermButtonVariant { Primary, Ghost, Soft, Danger }

@Composable
fun IntermButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: IntermButtonVariant = IntermButtonVariant.Primary,
    enabled: Boolean = true,
    fillWidth: Boolean = false,
    content: @Composable () -> Unit,
) {
    val c = IntermTheme.colors
    val (bg, fg, borderColor) = when (variant) {
        IntermButtonVariant.Primary -> Triple(c.primary, if (c.isDark) Color(0xFF14130F) else c.surface, null)
        IntermButtonVariant.Ghost -> Triple(Color.Transparent, c.ink, c.border)
        IntermButtonVariant.Soft -> Triple(c.primarySoft, c.primary, null)
        IntermButtonVariant.Danger -> Triple(c.accentSoft, c.accent, null)
    }
    val interaction = rememberInteractionSource()
    val shape = RoundedCornerShape(999.dp)
    Box(
        modifier = modifier
            .let { if (fillWidth) it.fillMaxWidth() else it }
            .height(52.dp)
            .pressScale(interactionSource = interaction)
            .clip(shape)
            .background(bg)
            .let { if (borderColor != null) it.border(1.dp, borderColor, shape) else it }
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides fg) {
            ProvideTextStyle(IntermTheme.typography.button.copy(color = fg)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                ) {
                    content()
                }
            }
        }
    }
}
