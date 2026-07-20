package xyz.jishnu.health.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import xyz.jishnu.health.domain.FastingCalendar
import xyz.jishnu.health.ui.theme.IntermTheme
import java.time.format.DateTimeFormatter

/** Level 0..4 intensity ramp, tuned per theme (index = CalendarDay.level). */
@Composable
fun calendarLevelColors(): List<Color> =
    if (IntermTheme.colors.isDark) {
        listOf(
            Color(0xFF2A2822), Color(0xFF2C4A3B), Color(0xFF35604D), Color(0xFF4E9576), Color(0xFF7DD3A8),
        )
    } else {
        listOf(
            Color(0xFFEAE6DB), Color(0xFFCBDDCF), Color(0xFF97BEA5), Color(0xFF598872), Color(0xFF2A4D3E),
        )
    }

@Composable
fun FastingCalendarCard(
    calendar: FastingCalendar,
    onDayClick: (dayKey: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = IntermTheme.colors
    if (calendar.weeks.isEmpty()) return
    val ramp = calendarLevelColors()
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val scroll = rememberScrollState()

    // Fixed cell geometry (dp).
    val cell = 13.dp
    val gap = 3.dp
    val gutter = 26.dp
    val monthH = 16.dp
    val rowStride = cell + gap
    val gridWidth = rowStride * calendar.weeks.size
    val gridHeight = monthH + rowStride * 7

    val monthStyle = IntermTheme.typography.mono.copy(fontSize = 9.5.sp, color = c.muted)
    val dowStyle = IntermTheme.typography.mono.copy(fontSize = 9.sp, color = c.muted)

    IntermCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "FASTING CALENDAR",
                style = IntermTheme.typography.hEyebrow,
                color = c.muted,
            )
            Spacer(Modifier.height(6.dp))
            val span = "${calendar.startDate.format(DateTimeFormatter.ofPattern("MMM yyyy"))} – " +
                calendar.endDate.format(DateTimeFormatter.ofPattern("MMM yyyy"))
            Text(text = span, style = IntermTheme.typography.caption, color = c.muted)

            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                CalStat(value = calendar.daysFasted.toString(), label = "Days fasted")
                CalStat(value = calendar.goalMetDays.toString(), label = "Goal met")
                CalStat(value = "${calendar.longestStreak}d", label = "Longest streak")
            }

            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.Top) {
                // Pinned weekday gutter (Mon / Wed / Fri).
                Canvas(
                    modifier = Modifier
                        .width(gutter)
                        .height(gridHeight),
                ) {
                    val strideP = rowStride.toPx()
                    val cellP = cell.toPx()
                    val top = monthH.toPx()
                    listOf(0 to "Mon", 2 to "Wed", 4 to "Fri").forEach { (rowIdx, label) ->
                        val measured = measurer.measure(AnnotatedString(label), dowStyle)
                        drawText(
                            measurer,
                            label,
                            topLeft = Offset(0f, top + rowIdx * strideP + (cellP - measured.size.height) / 2f),
                            style = dowStyle,
                        )
                    }
                }
                // Scrollable grid.
                Box(modifier = Modifier.horizontalScroll(scroll)) {
                    Canvas(
                        modifier = Modifier
                            .width(gridWidth)
                            .height(gridHeight)
                            .pointerInput(calendar.weeks.size) {
                                detectTapGestures { tap ->
                                    val strideP = rowStride.toPx()
                                    val top = monthH.toPx()
                                    if (tap.y < top) return@detectTapGestures
                                    val col = (tap.x / strideP).toInt()
                                    val row = ((tap.y - top) / strideP).toInt()
                                    if (col in calendar.weeks.indices && row in 0..6) {
                                        calendar.weeks[col][row]?.let { onDayClick(it.dayKey) }
                                    }
                                }
                            },
                    ) {
                        val strideP = rowStride.toPx()
                        val cellP = cell.toPx()
                        val top = monthH.toPx()
                        val radius = with(density) { 3.dp.toPx() }
                        // month labels
                        calendar.monthLabels.forEach { ml ->
                            drawText(
                                measurer,
                                ml.text,
                                topLeft = Offset(ml.weekIndex * strideP, 0f),
                                style = monthStyle,
                            )
                        }
                        // cells
                        calendar.weeks.forEachIndexed { col, week ->
                            week.forEachIndexed { row, day ->
                                if (day == null) return@forEachIndexed
                                val x = col * strideP
                                val y = top + row * strideP
                                drawRoundRect(
                                    color = ramp[day.level],
                                    topLeft = Offset(x, y),
                                    size = Size(cellP, cellP),
                                    cornerRadius = CornerRadius(radius, radius),
                                )
                                if (day.date == calendar.endDate) {
                                    drawRoundRect(
                                        color = c.accent,
                                        topLeft = Offset(x, y),
                                        size = Size(cellP, cellP),
                                        cornerRadius = CornerRadius(radius, radius),
                                        style = Stroke(width = with(density) { 1.5.dp.toPx() }),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Less", style = IntermTheme.typography.caption, color = c.muted)
                Spacer(Modifier.width(6.dp))
                ramp.forEach { color ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 2.dp)
                            .size(12.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(color),
                    )
                }
                Spacer(Modifier.width(6.dp))
                Text("More", style = IntermTheme.typography.caption, color = c.muted)
            }
        }
    }

    // Open scrolled to the most recent weeks.
    LaunchedEffect(calendar.weeks.size, scroll.maxValue) {
        if (scroll.maxValue > 0) scroll.scrollTo(scroll.maxValue)
    }
}

@Composable
private fun CalStat(value: String, label: String) {
    val c = IntermTheme.colors
    Column {
        Text(value, style = IntermTheme.typography.mono.copy(fontSize = 20.sp), color = c.ink)
        Text(label, style = IntermTheme.typography.hEyebrow, color = c.muted)
    }
}
