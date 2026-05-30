package xyz.jishnu.health.ui.screens.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.jishnu.health.data.model.DayEntry
import xyz.jishnu.health.data.model.Units
import xyz.jishnu.health.domain.WeightMath
import xyz.jishnu.health.ui.theme.IntermTheme
import kotlin.math.ceil
import kotlin.math.floor

@Composable
fun FastChart(
    entries: List<DayEntry>,
    units: Units,
    modifier: Modifier = Modifier,
) {
    val c = IntermTheme.colors
    val data = entries.sortedBy { it.dayKey }
    val weights = data.mapNotNull { it.weight?.weightKg }
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current

    val tickStyle = IntermTheme.typography.mono.copy(fontSize = 10.sp, color = c.muted)
    val rightTickStyle = tickStyle
    val xLabelStyle = tickStyle.copy(fontWeight = FontWeight.W400)

    val wMinKg = if (weights.isEmpty()) 60.0 else floor(weights.min() - 0.5)
    val wMaxKg = if (weights.isEmpty()) 100.0 else ceil(weights.max() + 0.5)

    Canvas(modifier = modifier.fillMaxWidth().height(220.dp)) {
        val w = size.width
        val h = size.height
        val padTop = with(density) { 14.dp.toPx() }
        val padBottom = with(density) { 24.dp.toPx() }
        val padLeft = with(density) { 36.dp.toPx() }
        val padRight = with(density) { 36.dp.toPx() }
        val innerLeft = padLeft
        val innerTop = padTop
        val innerRight = w - padRight
        val innerBottom = h - padBottom
        val innerW = innerRight - innerLeft
        val innerH = innerBottom - innerTop

        val grid = c.border
        fun xAt(i: Int): Float = if (data.size <= 1) innerLeft + innerW / 2f
        else innerLeft + (i.toFloat() / (data.size - 1)) * innerW

        fun wY(weightKg: Double): Float {
            val ratio = ((weightKg - wMinKg) / (wMaxKg - wMinKg)).coerceIn(0.0, 1.0)
            return innerTop + (1f - ratio.toFloat()) * innerH
        }

        fun fY(hoursValue: Double): Float {
            val ratio = (hoursValue / 24.0).coerceIn(0.0, 1.0)
            return innerTop + (1f - ratio.toFloat()) * innerH
        }

        // Horizontal gridlines for weight axis (5 ticks)
        val ticks = 4
        for (i in 0..ticks) {
            val vKg = wMinKg + (wMaxKg - wMinKg) * (i.toFloat() / ticks)
            val y = wY(vKg)
            val dashed = i != 0 && i != ticks
            drawLine(
                color = grid,
                start = Offset(innerLeft, y),
                end = Offset(innerRight, y),
                strokeWidth = 1f,
                pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(2f, 4f), 0f) else null,
            )
            val labelValue = if (units == Units.Imperial) WeightMath.kgToLb(vKg) else vKg
            val labelText = "${labelValue.toInt()}"
            val measured = measurer.measure(AnnotatedString(labelText), tickStyle)
            drawText(
                measurer,
                labelText,
                topLeft = Offset(innerLeft - measured.size.width - 6f, y - measured.size.height / 2f),
                style = tickStyle,
            )
        }

        // Right axis ticks at 0, 8, 16, 24
        for (v in listOf(0, 8, 16, 24)) {
            val y = fY(v.toDouble())
            val labelText = "${v}h"
            val measured = measurer.measure(AnnotatedString(labelText), rightTickStyle)
            drawText(
                measurer,
                labelText,
                topLeft = Offset(innerRight + 6f, y - measured.size.height / 2f),
                style = rightTickStyle,
            )
        }

        if (data.isNotEmpty()) {
            // Fasting hours line (accent, 85% opacity). Skip today before any
            // fast has been logged — that point would drag the line to 0.
            val accentColor = c.accent.copy(alpha = 0.85f)
            val fastPoints = data.mapIndexedNotNull { i, d ->
                if (d.isPreFastToday) null else i to d
            }
            val fastPath = Path().apply {
                fastPoints.forEachIndexed { idx, (i, d) ->
                    val x = xAt(i)
                    val y = fY(d.fastHours)
                    if (idx == 0) moveTo(x, y) else lineTo(x, y)
                }
            }
            drawPath(
                path = fastPath,
                color = accentColor,
                style = Stroke(width = with(density) { 1.8.dp.toPx() }),
            )
            fastPoints.forEach { (i, d) ->
                drawCircle(
                    color = c.accent,
                    radius = with(density) { 2.5.dp.toPx() },
                    center = Offset(xAt(i), fY(d.fastHours)),
                )
            }

            // Weight line (primary, 2.2dp)
            val weightPoints = data.mapIndexedNotNull { i, d -> d.weight?.let { i to it.weightKg } }
            if (weightPoints.isNotEmpty()) {
                val weightPath = Path().apply {
                    weightPoints.forEachIndexed { idx, (i, kg) ->
                        val x = xAt(i)
                        val y = wY(kg)
                        if (idx == 0) moveTo(x, y) else lineTo(x, y)
                    }
                }
                drawPath(
                    path = weightPath,
                    color = c.primary,
                    style = Stroke(width = with(density) { 2.2.dp.toPx() }),
                )
                weightPoints.forEach { (i, kg) ->
                    val center = Offset(xAt(i), wY(kg))
                    drawCircle(color = c.bg, radius = with(density) { 3.dp.toPx() }, center = center)
                    drawCircle(
                        color = c.primary,
                        radius = with(density) { 3.dp.toPx() },
                        center = center,
                        style = Stroke(width = with(density) { 2.dp.toPx() }),
                    )
                }
            }

            // X labels — every other point's day number
            data.forEachIndexed { i, d ->
                if (i % 2 == 0) {
                    val labelText = d.date.dayOfMonth.toString()
                    val measured = measurer.measure(AnnotatedString(labelText), xLabelStyle)
                    val x = xAt(i) - measured.size.width / 2f
                    drawText(
                        measurer,
                        labelText,
                        topLeft = Offset(x, h - padBottom + 4f),
                        style = xLabelStyle,
                    )
                }
            }
        }
    }
}
