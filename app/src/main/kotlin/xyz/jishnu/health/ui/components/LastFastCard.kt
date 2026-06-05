package xyz.jishnu.health.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.jishnu.health.R
import xyz.jishnu.health.data.constants.Stages
import xyz.jishnu.health.domain.StageCalculator
import xyz.jishnu.health.domain.TimeMath
import xyz.jishnu.health.ui.theme.IntermTheme
import xyz.jishnu.health.ui.theme.stageColors
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

/**
 * Snapshot of a completed fast as needed by [LastFastCard]. The card never
 * looks at the underlying entity, so this is what callers prepare.
 */
data class LastFastSummary(
    val startMs: Long,
    val endMs: Long,
    val durationHours: Double,
    val goalHours: Int,
    val planLabel: String,
)

/**
 * Editable handles for the Started/Ended labels in [LastFastCard]. When
 * non-null, tapping either marker opens a time picker and the new wall-clock
 * value is propagated up so callers can rebuild a [LastFastSummary] for the
 * edited day.
 */
data class LastFastEdit(
    val startTime: String,
    val endTime: String,
    val onStart: (String) -> Unit,
    val onEnd: (String) -> Unit,
)

private val HeaderGradientStops = arrayOf(
    0.0f to Color(0xFF2A4D3E),
    0.46f to Color(0xFF3D6B56),
    0.92f to Color(0xFFC46A45),
    1.0f to Color(0xFFCC7348),
)

private val Cream = Color(0xFFFDFBF7)
private val CreamMuted = Color(0xCBFDFBF7) // ≈ rgba(253,251,247,0.80)
private val CreamSubtle = Color(0xB8FDFBF7) // ≈ rgba(253,251,247,0.72)
private val CreamFaint = Color(0xA6FDFBF7) // ≈ rgba(253,251,247,0.65)

/**
 * Recap of the most recent completed fast — dark-gradient header band with the
 * brand lockup, share button, date, duration headline and goal chip, plus a
 * body section showing the stage reached, the 24h stage ribbon filled to the
 * duration, and the wall-clock window. Mirrors `LastFastCard` in
 * `docs/project/shared.jsx`. Optionally shows a share button and exposes
 * editable time markers for day detail.
 */
@Composable
fun LastFastCard(
    summary: LastFastSummary,
    modifier: Modifier = Modifier,
    edit: LastFastEdit? = null,
    onShare: (() -> Unit)? = null,
    /**
     * When true the card renders in its share-image form: no share button, no
     * edit affordances, and an "INTERMITTENT FASTING" eyebrow takes the share
     * button's slot so the shared screenshot reads as a standalone artifact.
     */
    forCapture: Boolean = false,
) {
    val c = IntermTheme.colors
    val palette = stageColors()
    val durH = summary.durationHours
    val hh = durH.toInt()
    val mm = ((durH - hh) * 60).toInt()
    val pct = (if (summary.goalHours > 0) (durH / summary.goalHours) * 100 else 0.0).toInt()
    val goalMet = durH >= summary.goalHours

    val reached = StageCalculator.stageFor(durH, Stages.all)
    val reachedColor = palette[reached.id] ?: c.primary

    val zone = ZoneId.systemDefault()
    val start = Instant.ofEpochMilli(summary.startMs)
    val end = Instant.ofEpochMilli(summary.endMs)
    val dayLabel = DateTimeFormatter
        .ofPattern("EEEE, MMM d", Locale.getDefault())
        .format(end.atZone(zone).toLocalDate())

    val segs = Stages.all.filter { it.startHour < 24 }.let { pool ->
        pool.mapIndexed { idx, s ->
            val e = pool.getOrNull(idx + 1)?.startHour?.toFloat() ?: 24f
            Triple(s.id, s.startHour.toFloat(), e)
        }
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(c.card),
    ) {
        HeaderBand(
            dayLabel = dayLabel,
            hh = hh,
            mm = mm,
            pct = pct,
            goalMet = goalMet,
            planLabel = summary.planLabel,
            onShare = onShare,
            forCapture = forCapture,
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("STAGE REACHED", style = IntermTheme.typography.hEyebrow, color = c.muted)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(reachedColor),
                    )
                    Text(
                        reached.name,
                        style = IntermTheme.typography.caption.copy(fontSize = 12.sp, fontWeight = FontWeight.W500),
                        color = c.ink,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth().height(8.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                segs.forEach { (id, s, e) ->
                    val span = (e - s).coerceAtLeast(0.001f)
                    val segColor = palette[id] ?: c.primary
                    val fill = ((durH - s) / span).coerceIn(0.0, 1.0).toFloat()
                    Box(
                        modifier = Modifier
                            .weight(span)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp))
                            .background(segColor.copy(alpha = 0.18f)),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fill)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(4.dp))
                                .background(segColor),
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(c.border))
            Spacer(Modifier.height(12.dp))

            val startTimeLabel = TimeMath.fmtTime(start, zone)
            val endTimeLabel = TimeMath.fmtTime(end, zone)

            var showStartPicker by remember { mutableStateOf(false) }
            var showEndPicker by remember { mutableStateOf(false) }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                MarkerColumn(
                    label = "Started",
                    value = startTimeLabel,
                    align = Alignment.Start,
                    onClick = edit?.let { { showStartPicker = true } },
                )
                Row(
                    modifier = Modifier.weight(1f).widthIn(min = 60.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Box(modifier = Modifier.height(1.dp).background(c.border).widthIn(min = 24.dp, max = 40.dp).fillMaxWidth())
                    Spacer(Modifier.size(6.dp))
                    Icon(IntermIcons.Chevron, contentDescription = null, tint = c.subtle, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.size(6.dp))
                    Box(modifier = Modifier.height(1.dp).background(c.border).widthIn(min = 24.dp, max = 40.dp).fillMaxWidth())
                }
                MarkerColumn(
                    label = "Ended",
                    value = endTimeLabel,
                    align = Alignment.End,
                    onClick = edit?.let { { showEndPicker = true } },
                )
            }

            if (edit != null && showStartPicker) {
                TimePickerDialog(
                    initial = edit.startTime,
                    onDismiss = { showStartPicker = false },
                    onConfirm = {
                        showStartPicker = false
                        edit.onStart(it)
                    },
                )
            }
            if (edit != null && showEndPicker) {
                TimePickerDialog(
                    initial = edit.endTime,
                    onDismiss = { showEndPicker = false },
                    onConfirm = {
                        showEndPicker = false
                        edit.onEnd(it)
                    },
                )
            }
        }
    }
}

@Composable
private fun HeaderBand(
    dayLabel: String,
    hh: Int,
    mm: Int,
    pct: Int,
    goalMet: Boolean,
    planLabel: String,
    onShare: (() -> Unit)?,
    forCapture: Boolean,
) {
    val gradient = remember {
        Brush.linearGradient(
            colorStops = HeaderGradientStops,
            start = Offset.Zero,
            end = Offset.Infinite,
        )
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clipToBounds()
            .background(gradient),
    ) {
        // The motif sits in the header's top-right corner, overflowing past
        // both edges; align(TopEnd) anchors the right edge, then
        // absoluteOffset(x = 22.dp, y = -26.dp) pushes it 22dp past the right
        // and 26dp past the top — matching the prototype's right:-22 / top:-26.
        // The parent's clipToBounds trims the overflow.
        BrandMotif(
            modifier = Modifier
                .size(132.dp)
                .align(Alignment.TopEnd)
                .absoluteOffset(x = 22.dp, y = (-26).dp)
                .alpha(0.18f),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 18.dp, top = 15.dp, bottom = 18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_brand_dark),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                    )
                    Text(
                        "Fast",
                        style = IntermTheme.typography.body.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.W600,
                            letterSpacing = (-0.01f).sp,
                        ),
                        color = Cream,
                    )
                }
                when {
                    forCapture -> Text(
                        "INTERMITTENT FASTING",
                        style = IntermTheme.typography.hEyebrow.copy(fontSize = 10.5.sp),
                        color = CreamSubtle,
                    )
                    onShare != null -> Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0x29FFFFFF)) // ≈ rgba(255,255,255,0.16)
                            .clickable(onClick = onShare),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            IntermIcons.Share,
                            contentDescription = "Share",
                            tint = Cream,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(15.dp))
            Text(
                dayLabel.uppercase(),
                style = IntermTheme.typography.hEyebrow.copy(
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.W600,
                    letterSpacing = 1.45.sp,
                ),
                color = CreamSubtle,
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    hh.toString(),
                    style = IntermTheme.typography.hDisplay.copy(
                        fontSize = 46.sp,
                        fontWeight = FontWeight.W400,
                        lineHeight = 46.sp,
                        letterSpacing = (-1.4f).sp,
                    ),
                    color = Cream,
                )
                Text(
                    "h",
                    style = IntermTheme.typography.hDisplay.copy(fontSize = 23.sp, lineHeight = 23.sp),
                    color = CreamFaint,
                    modifier = Modifier.padding(start = 2.dp, bottom = 6.dp),
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    mm.toString().padStart(2, '0'),
                    style = IntermTheme.typography.hDisplay.copy(
                        fontSize = 46.sp,
                        fontWeight = FontWeight.W400,
                        lineHeight = 46.sp,
                        letterSpacing = (-1.4f).sp,
                    ),
                    color = Cream,
                )
                Text(
                    "m",
                    style = IntermTheme.typography.hDisplay.copy(fontSize = 23.sp, lineHeight = 23.sp),
                    color = CreamFaint,
                    modifier = Modifier.padding(start = 2.dp, bottom = 6.dp),
                )
            }
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0x2BFFFFFF)) // ≈ rgba(255,255,255,0.17)
                    .padding(horizontal = 11.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (goalMet) {
                    Icon(
                        IntermIcons.Check,
                        contentDescription = null,
                        tint = Cream,
                        modifier = Modifier.size(14.dp),
                    )
                }
                Text(
                    "$planLabel · ${if (goalMet) "Goal reached" else "$pct% of goal"}",
                    style = IntermTheme.typography.caption.copy(fontSize = 12.sp, fontWeight = FontWeight.W500),
                    color = Cream,
                )
            }
        }
    }
}

/**
 * The cream concentric-rings + radial-ticks motif that overflows the
 * top-right corner of the header band, mirroring the SVG in the prototype's
 * LastFastCard. Drawn at the prototype's 100×100 viewBox so the geometry
 * (three rings + 24 ticks with every 6th being longer) stays exact.
 */
@Composable
private fun BrandMotif(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val cream = Cream
        val unit = size.minDimension / 100f
        val cx = 50f * unit
        val cy = 50f * unit
        val strokeBase = 0.8f * unit
        listOf(46f, 34f, 22f).forEach { radius ->
            drawCircle(
                color = cream,
                center = Offset(cx, cy),
                radius = radius * unit,
                style = Stroke(width = strokeBase),
            )
        }
        for (i in 0 until 24) {
            val angle = (i.toDouble() / 24.0) * 2 * Math.PI - Math.PI / 2
            val isCardinal = i % 6 == 0
            val inner = 46f
            val outer = inner + if (isCardinal) 5f else 2.6f
            val tickWidth = if (isCardinal) 1.1f else 0.6f
            val x1 = cx + cos(angle).toFloat() * inner * unit
            val y1 = cy + sin(angle).toFloat() * inner * unit
            val x2 = cx + cos(angle).toFloat() * outer * unit
            val y2 = cy + sin(angle).toFloat() * outer * unit
            drawLine(
                color = cream,
                start = Offset(x1, y1),
                end = Offset(x2, y2),
                strokeWidth = tickWidth * unit,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun MarkerColumn(
    label: String,
    value: String,
    align: Alignment.Horizontal,
    onClick: (() -> Unit)?,
) {
    val c = IntermTheme.colors
    val editable = onClick != null
    // No horizontal padding — the column's start (or end) edge needs to align
    // with the "STAGE REACHED" eyebrow / stage chip in the rows above. Vertical
    // padding stays so the tap target has some breathing room when editable.
    Column(
        horizontalAlignment = align,
        modifier = if (editable) {
            Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onClick!!)
                .padding(vertical = 4.dp)
        } else {
            Modifier.padding(vertical = 4.dp)
        },
    ) {
        Text(label, style = IntermTheme.typography.caption, color = c.muted)
        Spacer(Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                value,
                style = IntermTheme.typography.mono.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.W500,
                    textDecoration = if (editable) androidx.compose.ui.text.style.TextDecoration.Underline else null,
                ),
                color = if (editable) c.primary else c.ink,
            )
            if (editable) {
                Icon(
                    IntermIcons.ChevronDown,
                    contentDescription = "Edit",
                    tint = c.primary,
                    modifier = Modifier.size(12.dp),
                )
            }
        }
    }
}

/**
 * Helper to rebuild a [LastFastSummary] from edited Started/Ended wall-clock
 * times anchored to the supplied date. If the end time is earlier than the
 * start time we treat it as wrapping across midnight (the day-detail screen
 * shows the day a fast *ends*, so a 22:00→06:00 window is valid).
 */
fun rebuildSummary(
    onDate: LocalDate,
    startHhmm: String,
    endHhmm: String,
    goalHours: Int,
    planLabel: String,
    zone: ZoneId = ZoneId.systemDefault(),
): LastFastSummary {
    val startLocal = LocalDateTime.of(onDate, LocalTime.parse(startHhmm))
    val endLocal = LocalDateTime.of(onDate, LocalTime.parse(endHhmm))
    val wrappedEnd = if (endLocal.isBefore(startLocal)) endLocal.plusDays(1) else endLocal
    val startMs = startLocal.atZone(zone).toInstant().toEpochMilli()
    val endMs = wrappedEnd.atZone(zone).toInstant().toEpochMilli()
    val durationH = (endMs - startMs) / 3_600_000.0
    return LastFastSummary(
        startMs = startMs,
        endMs = endMs,
        durationHours = durationH,
        goalHours = goalHours,
        planLabel = planLabel,
    )
}
