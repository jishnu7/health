package xyz.jishnu.health.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import xyz.jishnu.health.ui.theme.IntermTheme

@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 240.dp,
    stroke: Dp = 14.dp,
    color: Color = IntermTheme.colors.primary,
    track: Color = IntermTheme.colors.border,
    dashed: Boolean = false,
    dashAmount: Float? = null,
    content: @Composable () -> Unit = {},
) {
    val clamped = progress.coerceIn(0f, 1f)
    val dashRatio = (dashAmount ?: if (dashed) 1f else 0f).coerceIn(0f, 1f)
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            val strokePx = stroke.toPx()
            val diameter = this.size.minDimension - strokePx
            val topLeft = Offset((this.size.width - diameter) / 2f, (this.size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)
            val pathEffect = if (dashRatio > 0.01f) {
                // Interpolate the dash period so a wipe→solid transition smoothly
                // closes the gaps instead of swapping the path effect on/off.
                val dashOn = 2f + 6f * (1f - dashRatio)
                val dashOff = 6f * dashRatio
                PathEffect.dashPathEffect(floatArrayOf(dashOn, dashOff), 0f)
            } else {
                null
            }
            val cap = if (dashRatio > 0.5f) StrokeCap.Butt else StrokeCap.Round
            drawArc(
                color = track,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Butt, pathEffect = pathEffect),
            )
            if (clamped > 0f) {
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f * clamped,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = cap, pathEffect = pathEffect),
                )
            }
        }
        content()
    }
}
