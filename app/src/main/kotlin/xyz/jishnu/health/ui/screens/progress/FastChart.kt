package xyz.jishnu.health.ui.screens.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.format.DateTimeFormatter
import java.util.Locale
import xyz.jishnu.health.data.constants.Stages
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
    goalHours: Int,
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

    // Index of the day whose tooltip is currently shown. Cleared when the
    // user taps it again or the data list changes.
    var selectedIdx by remember(data.size) { mutableStateOf<Int?>(null) }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .pointerInput(data.size) {
                detectTapGestures { tap ->
                    if (data.isEmpty()) return@detectTapGestures
                    val padLeftPx = 36.dp.toPx()
                    val padRightPx = 36.dp.toPx()
                    val innerLeft = padLeftPx
                    val innerRight = size.width - padRightPx
                    val innerW = innerRight - innerLeft
                    val idx = (data.indices).minByOrNull { i ->
                        val x = if (data.size <= 1) innerLeft + innerW / 2f
                        else innerLeft + (i.toFloat() / (data.size - 1)) * innerW
                        kotlin.math.abs(x - tap.x)
                    } ?: return@detectTapGestures
                    selectedIdx = if (selectedIdx == idx) null else idx
                }
            },
    ) {
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

        // Stage boundaries — each metabolic stage gets a faint gridline + a
        // tick + an hour label on the right axis. Anchor hours (0/8/16/24)
        // render at the default size; secondary stages (4/12/14/20) use a
        // smaller label. The user's plan goal gets its own emphasised line
        // (drawn below) and is intentionally skipped here so the two visuals
        // don't double up at the same y.
        val anchorHours = setOf(0, 8, 16, 24)
        val minorStageStyle = rightTickStyle.copy(fontSize = 9.sp)
        for (h in Stages.all.map { it.startHour }) {
            if (h == goalHours) continue
            // 14h (Fat burn stage start) used to get its own emphasis line but
            // we now reserve that visual treatment for the user's goal — skip
            // the boundary entirely so it doesn't clutter the axis.
            if (h == 14) continue
            val y = fY(h.toDouble())
            val isAnchor = h in anchorHours
            if (h in 1..23) {
                drawLine(
                    color = c.accent.copy(alpha = 0.18f),
                    start = Offset(innerLeft, y),
                    end = Offset(innerRight, y),
                    strokeWidth = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f, 4f), 0f),
                )
            }
            drawLine(
                color = if (isAnchor) c.muted else c.subtle,
                start = Offset(innerRight, y),
                end = Offset(innerRight + if (isAnchor) 5f else 3f, y),
                strokeWidth = 1f,
            )
            val labelText = "${h}h"
            val style = if (isAnchor) rightTickStyle else minorStageStyle
            val measured = measurer.measure(AnnotatedString(labelText), style)
            drawText(
                measurer,
                labelText,
                topLeft = Offset(innerRight + 6f, y - measured.size.height / 2f),
                style = style,
            )
        }

        // Plan goal — dedicated horizontal line across the chart at the user's
        // fast goal so they can see at a glance which days cleared the bar.
        if (goalHours in 1..23) {
            val y = fY(goalHours.toDouble())
            drawLine(
                color = c.primary.copy(alpha = 0.55f),
                start = Offset(innerLeft, y),
                end = Offset(innerRight, y),
                strokeWidth = 1.2f,
            )
            val labelText = "${goalHours}h"
            val style = rightTickStyle.copy(
                fontSize = 9.sp,
                color = c.primary.copy(alpha = 0.8f),
                fontWeight = FontWeight.W500,
            )
            val measured = measurer.measure(AnnotatedString(labelText), style)
            drawText(
                measurer,
                labelText,
                topLeft = Offset(innerRight + 6f, y - measured.size.height / 2f),
                style = style,
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

            // Tap tooltip — vertical guide + highlighted dots + a card with
            // that day's fasting hours and weight reading.
            selectedIdx?.takeIf { it in data.indices }?.let { idx ->
                val d = data[idx]
                val x = xAt(idx)

                drawLine(
                    color = c.ink2.copy(alpha = 0.35f),
                    start = Offset(x, innerTop),
                    end = Offset(x, innerBottom),
                    strokeWidth = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 3f), 0f),
                )
                if (!d.isPreFastToday) {
                    drawCircle(
                        color = c.accent,
                        radius = with(density) { 4.5.dp.toPx() },
                        center = Offset(x, fY(d.fastHours)),
                    )
                }
                d.weight?.weightKg?.let { kg ->
                    drawCircle(
                        color = c.primary,
                        radius = with(density) { 5.dp.toPx() },
                        center = Offset(x, wY(kg)),
                    )
                }

                val dateText = d.date.format(
                    DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault()),
                )
                val fastH = if (d.isPreFastToday) {
                    "—"
                } else {
                    val hh = d.fastHours.toInt()
                    val mm = ((d.fastHours - hh) * 60).toInt()
                    "${hh}h ${mm.toString().padStart(2, '0')}m"
                }
                val weightStr = d.weight?.weightKg?.let { kg ->
                    val fmt = WeightMath.fmtWeight(kg, units)
                    "${fmt.value} ${fmt.unit}"
                } ?: "—"

                val tooltipStyle = TextStyle(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.W500,
                    color = c.ink,
                )
                val tooltipText = buildAnnotatedString {
                    withStyle(SpanStyle(color = c.muted, fontWeight = FontWeight.W500)) {
                        append(dateText.uppercase())
                    }
                    append("\n")
                    withStyle(SpanStyle(color = c.accent)) { append("Fast  ") }
                    append(fastH)
                    append("\n")
                    withStyle(SpanStyle(color = c.primary)) { append("Weight  ") }
                    append(weightStr)
                }
                val tooltipMeasured = measurer.measure(tooltipText, tooltipStyle)

                val pad = with(density) { 10.dp.toPx() }
                val tooltipW = tooltipMeasured.size.width + 2 * pad
                val tooltipH = tooltipMeasured.size.height + 2 * pad
                val gap = with(density) { 6.dp.toPx() }
                var tooltipX = x - tooltipW / 2f
                if (tooltipX < innerLeft) tooltipX = innerLeft
                if (tooltipX + tooltipW > innerRight) tooltipX = innerRight - tooltipW
                val tooltipY = innerTop + gap

                drawRoundRect(
                    color = c.card,
                    topLeft = Offset(tooltipX, tooltipY),
                    size = Size(tooltipW, tooltipH),
                    cornerRadius = CornerRadius(10f, 10f),
                )
                drawRoundRect(
                    color = c.border,
                    topLeft = Offset(tooltipX, tooltipY),
                    size = Size(tooltipW, tooltipH),
                    cornerRadius = CornerRadius(10f, 10f),
                    style = Stroke(width = 1f),
                )
                drawText(
                    measurer,
                    tooltipText,
                    topLeft = Offset(tooltipX + pad, tooltipY + pad),
                    style = tooltipStyle,
                )
            }
        }
    }
}
