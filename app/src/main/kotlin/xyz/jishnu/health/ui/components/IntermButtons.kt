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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.jishnu.health.ui.theme.IntermTheme

enum class IntermButtonVariant { Primary, Ghost, Soft, Danger }

/**
 * Vertical size of the button. Medium is the standard 52dp pill; Large is a
 * 64dp pill used for primary "commit" calls-to-action on splash-y surfaces
 * (Home idle, Weight log).
 */
enum class IntermButtonSize(
    val height: Dp,
    val horizontalPadding: Dp,
    val fontSize: TextUnit,
    val iconGap: Dp,
) {
    Medium(height = 52.dp, horizontalPadding = 24.dp, fontSize = 15.sp, iconGap = 8.dp),
    Large(height = 64.dp, horizontalPadding = 36.dp, fontSize = 17.sp, iconGap = 10.dp),
}

@Composable
fun IntermButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: IntermButtonVariant = IntermButtonVariant.Primary,
    size: IntermButtonSize = IntermButtonSize.Medium,
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
            .height(size.height)
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
            .padding(horizontal = size.horizontalPadding),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides fg) {
            ProvideTextStyle(IntermTheme.typography.button.copy(color = fg, fontSize = size.fontSize)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(size.iconGap, Alignment.CenterHorizontally),
                ) {
                    content()
                }
            }
        }
    }
}
