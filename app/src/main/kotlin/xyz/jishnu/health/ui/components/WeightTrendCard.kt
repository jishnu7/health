package xyz.jishnu.health.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
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
import xyz.jishnu.health.data.local.WeightEntryEntity
import xyz.jishnu.health.data.model.Units
import xyz.jishnu.health.domain.WeightMath
import xyz.jishnu.health.ui.theme.IntermTheme
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs

/**
 * Weekly weight trend chart. Each week becomes a vertical band spanning the
 * range from min to max for that week, and a smoothed cardinal spline runs
 * through each week's average. Mirrors `WeightTrendCard` in
 * `docs/project/shared.jsx`.
 *
 * The card takes the user's most-recent weight entries and slices them into
 * ISO weeks. If fewer than [weeks] weeks of data exist, only the weeks we
 * have are drawn — there's no synthetic filler.
 */
@Composable
fun WeightTrendCard(
    entries: List<WeightEntryEntity>,
    units: Units,
    modifier: Modifier = Modifier,
    weeks: Int = 8,
) {
    val c = IntermTheme.colors
    val weeklyAll = buildWeekly(entries)
    if (weeklyAll.isEmpty()) return
    val weekly = weeklyAll.takeLast(weeks)

    val lastDisp = WeightMath.fmtWeight(weekly.last().avgKg, units)
    val unitLabel = lastDisp.unit
    val firstAvgDisp = displayWeight(weekly.first().avgKg, units)
    val lastAvgDisp = displayWeight(weekly.last().avgKg, units)
    val change = lastAvgDisp - firstAvgDisp
    val changeFmt = WeightMath.fmtWeight(
        weightKg = WeightMath.deltaToKg(abs(change).toDouble(), units),
        units = units,
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(c.card)
            .padding(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column {
                Text("WEIGHT TREND", style = IntermTheme.typography.hEyebrow, color = c.muted)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        lastDisp.value,
                        style = IntermTheme.typography.hDisplay.copy(fontSize = 30.sp),
                        color = c.ink,
                    )
                    Spacer(Modifier.size(6.dp))
                    Text(
                        unitLabel,
                        style = IntermTheme.typography.body.copy(fontSize = 15.sp),
                        color = c.muted,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${weekly.size}-week change",
                    style = IntermTheme.typography.caption,
                    color = c.muted,
                )
                Spacer(Modifier.height(4.dp))
                val changeColor = if (change < 0) c.primary else if (change > 0) c.accent else c.ink
                val changeSign = if (change < 0) "−" else if (change > 0) "+" else ""
                Text(
                    "$changeSign${changeFmt.value} ${changeFmt.unit}",
                    style = IntermTheme.typography.mono.copy(fontSize = 15.sp, fontWeight = FontWeight.W500),
                    color = changeColor,
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        TrendChart(weekly = weekly, units = units)

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LegendChip(
                swatch = {
                    Box(
                        modifier = Modifier
                            .size(width = 12.dp, height = 10.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(c.primary.copy(alpha = 0.16f)),
                    )
                },
                label = "Weekly range",
            )
            LegendChip(
                swatch = {
                    Box(
                        modifier = Modifier
                            .size(width = 16.dp, height = 2.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(c.primary),
                    )
                },
                label = "Average",
            )
        }
    }
}

@Composable
private fun LegendChip(swatch: @Composable () -> Unit, label: String) {
    val c = IntermTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        swatch()
        Text(label, style = IntermTheme.typography.caption, color = c.muted)
    }
}

@Composable
private fun TrendChart(weekly: List<WeeklyStat>, units: Units) {
    val c = IntermTheme.colors
    val rangeColor = c.primary.copy(alpha = 0.16f)
    val lineColor = c.primary
    val borderColor = c.border
    val mutedColor = c.muted
    val cardColor = c.card
    val inkColor = c.ink

    val disp = weekly.map { w ->
        val lo = displayWeight(w.minKg, units)
        val hi = displayWeight(w.maxKg, units)
        val avg = displayWeight(w.avgKg, units)
        DisplayWeek(lo = lo, hi = hi, avg = avg, date = w.weekStart)
    }
    val allLo = (disp.minOf { it.lo } - 1f).toFloat()
    val allHi = (disp.maxOf { it.hi } + 1f).toFloat()
    val ticks = 3
    val tickValues = (0..ticks).map { allLo + (allHi - allLo) * (it.toFloat() / ticks) }
    val unitLabel = if (units == Units.Metric) "kg" else "lb"
    val measurer = rememberTextMeasurer()

    var selectedIdx by remember(weekly.size) { mutableStateOf<Int?>(null) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(336f / 180f),
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(336f / 180f)
                .pointerInput(disp.size) {
                    detectTapGestures { tap ->
                        if (disp.isEmpty()) return@detectTapGestures
                        val w = size.width.toFloat()
                        val padLeftPx = w * (30f / 336f)
                        val padRightPx = w * (12f / 336f)
                        val iWPx = w - padLeftPx - padRightPx
                        val n = disp.size
                        val slotPx = iWPx / n
                        val idx = (disp.indices).minByOrNull { i ->
                            val x = padLeftPx + slotPx * (i + 0.5f)
                            kotlin.math.abs(x - tap.x)
                        } ?: return@detectTapGestures
                        selectedIdx = if (selectedIdx == idx) null else idx
                    }
                },
        ) {
            val w = size.width
            val h = size.height
            val padL = w * (30f / 336f)
            val padR = w * (12f / 336f)
            val padT = h * (12f / 180f)
            val padB = h * (22f / 180f)
            val iW = w - padL - padR
            val iH = h - padT - padB

            fun yAt(v: Float): Float = padT + (1f - (v - allLo) / (allHi - allLo)) * iH

            tickValues.forEachIndexed { idx, v ->
                val y = yAt(v)
                drawLine(
                    color = borderColor,
                    start = Offset(padL, y),
                    end = Offset(w - padR, y),
                    strokeWidth = 1f,
                    pathEffect = if (idx == ticks) null else androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(2f, 4f)),
                )
            }

            val n = disp.size
            val slot = iW / n
            val bw = minOf(16f, slot * 0.5f)
            fun cx(i: Int): Float = padL + slot * (i + 0.5f)

            disp.forEachIndexed { i, week ->
                val yHi = yAt(week.hi)
                val yLo = yAt(week.lo)
                val height = (yLo - yHi).coerceAtLeast(2f)
                drawRoundRect(
                    color = rangeColor,
                    topLeft = Offset(cx(i) - bw / 2, yHi),
                    size = Size(bw, height),
                    cornerRadius = CornerRadius(bw / 2, bw / 2),
                )
            }

            val avgPts = disp.mapIndexed { i, week -> Offset(cx(i), yAt(week.avg)) }
            if (avgPts.size >= 2) {
                val path = Path().apply {
                    moveTo(avgPts[0].x, avgPts[0].y)
                    for (i in 0 until avgPts.size - 1) {
                        val p0 = avgPts.getOrElse(i - 1) { avgPts[i] }
                        val p1 = avgPts[i]
                        val p2 = avgPts[i + 1]
                        val p3 = avgPts.getOrElse(i + 2) { p2 }
                        val c1x = p1.x + (p2.x - p0.x) / 6f
                        val c1y = p1.y + (p2.y - p0.y) / 6f
                        val c2x = p2.x - (p3.x - p1.x) / 6f
                        val c2y = p2.y - (p3.y - p1.y) / 6f
                        cubicTo(c1x, c1y, c2x, c2y, p2.x, p2.y)
                    }
                }
                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(width = 2.2f, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round),
                )
            }
            avgPts.forEach { p ->
                drawCircle(color = cardColor, radius = 3.2f, center = p)
                drawCircle(color = lineColor, radius = 3.2f, center = p, style = Stroke(width = 2f))
            }

            val labelFmt = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
            val labelPaint = android.graphics.Paint().apply {
                color = mutedColor.toArgb()
                textSize = h * (9f / 180f)
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
            }
            val tickPaint = android.graphics.Paint().apply {
                color = mutedColor.toArgb()
                textSize = h * (9f / 180f)
                textAlign = android.graphics.Paint.Align.RIGHT
                isAntiAlias = true
            }
            drawIntoCanvas { canvas ->
                val native = canvas.nativeCanvas
                disp.forEachIndexed { i, week ->
                    if (i % 2 == 0 || n <= 6) {
                        native.drawText(labelFmt.format(week.date), cx(i), h - h * (6f / 180f), labelPaint)
                    }
                }
                tickValues.forEach { v ->
                    val y = yAt(v)
                    native.drawText("${v.toInt()}", padL - 6f, y + 3f, tickPaint)
                }
            }

            // Tap tooltip — vertical guide + emphasised average dot + a card
            // with the week's range and average.
            selectedIdx?.takeIf { it in disp.indices }?.let { idx ->
                val week = disp[idx]
                val pointX = cx(idx)
                val pointY = yAt(week.avg)

                drawLine(
                    color = mutedColor.copy(alpha = 0.45f),
                    start = Offset(pointX, padT),
                    end = Offset(pointX, padT + iH),
                    strokeWidth = 1f,
                    pathEffect = androidx.compose.ui.graphics.PathEffect
                        .dashPathEffect(floatArrayOf(3f, 3f), 0f),
                )
                drawCircle(color = cardColor, radius = 5f, center = Offset(pointX, pointY))
                drawCircle(
                    color = lineColor,
                    radius = 5f,
                    center = Offset(pointX, pointY),
                    style = Stroke(width = 2.4f),
                )

                val weekLabel = labelFmt.format(week.date)
                val avgText = "${"%.1f".format(Locale.US, week.avg)} $unitLabel"
                val rangeText = "${"%.1f".format(Locale.US, week.lo)}–${"%.1f".format(Locale.US, week.hi)} $unitLabel"
                val tooltipStyle = TextStyle(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.W500,
                    color = inkColor,
                )
                val tooltipText = buildAnnotatedString {
                    withStyle(SpanStyle(color = mutedColor, fontWeight = FontWeight.W500)) {
                        append("WEEK OF ${weekLabel.uppercase()}")
                    }
                    append("\n")
                    withStyle(SpanStyle(color = lineColor)) { append("Avg  ") }
                    append(avgText)
                    append("\n")
                    withStyle(SpanStyle(color = mutedColor)) { append("Range  ") }
                    append(rangeText)
                }
                val measured = measurer.measure(tooltipText, tooltipStyle)
                val pad = 10f
                val tooltipW = measured.size.width + pad * 2f
                val tooltipH = measured.size.height + pad * 2f
                var tooltipX = pointX - tooltipW / 2f
                val minX = padL
                val maxX = w - padR - tooltipW
                if (tooltipX < minX) tooltipX = minX
                if (tooltipX > maxX) tooltipX = maxX
                val tooltipY = padT + 4f
                drawRoundRect(
                    color = cardColor,
                    topLeft = Offset(tooltipX, tooltipY),
                    size = Size(tooltipW, tooltipH),
                    cornerRadius = CornerRadius(10f, 10f),
                )
                drawRoundRect(
                    color = borderColor,
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

private data class WeeklyStat(
    val weekStart: LocalDate,
    val minKg: Double,
    val maxKg: Double,
    val avgKg: Double,
)

private data class DisplayWeek(val lo: Float, val hi: Float, val avg: Float, val date: LocalDate)

private fun buildWeekly(entries: List<WeightEntryEntity>): List<WeeklyStat> {
    if (entries.isEmpty()) return emptyList()
    val zone = ZoneId.systemDefault()
    val withDates = entries.map { entry ->
        val date = Instant.ofEpochMilli(entry.dayKey).atZone(zone).toLocalDate()
        date to entry.weightKg
    }.sortedBy { it.first }
    val groups = withDates.groupBy { (date, _) -> isoWeekStart(date) }
        .toSortedMap()
    return groups.map { (weekStart, list) ->
        val kgs = list.map { it.second }
        WeeklyStat(
            weekStart = weekStart,
            minKg = kgs.min(),
            maxKg = kgs.max(),
            avgKg = kgs.average(),
        )
    }
}

private fun isoWeekStart(date: LocalDate): LocalDate =
    date.minus(((date.dayOfWeek.value - DayOfWeek.MONDAY.value) + 7) % 7L, ChronoUnit.DAYS)

private fun displayWeight(weightKg: Double, units: Units): Float = when (units) {
    Units.Metric -> weightKg.toFloat()
    Units.Imperial -> WeightMath.kgToLb(weightKg).toFloat()
}

private fun Color.toArgb(): Int = android.graphics.Color.argb(
    (alpha * 255).toInt(),
    (red * 255).toInt(),
    (green * 255).toInt(),
    (blue * 255).toInt(),
)
