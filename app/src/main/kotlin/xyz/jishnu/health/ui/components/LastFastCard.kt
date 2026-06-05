package xyz.jishnu.health.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

/**
 * Recap of the most recent completed fast — duration headline, goal-met chip,
 * stage reached, 24h stage ribbon filled to the duration, and the wall-clock
 * window. Mirrors `LastFastCard` in `docs/project/shared.jsx`. Optionally
 * shows a share button and exposes editable time markers for day detail.
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(lerp(c.card, c.primary, 0.22f))
                .padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    dayLabel.uppercase(),
                    style = IntermTheme.typography.hEyebrow,
                    color = c.muted,
                )
                when {
                    forCapture -> Text(
                        "INTERMITTENT FASTING",
                        style = IntermTheme.typography.hEyebrow,
                        color = c.muted,
                    )
                    onShare != null -> Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onShare),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            IntermIcons.Share,
                            contentDescription = "Share",
                            tint = c.primary,
                            modifier = Modifier.size(17.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    hh.toString(),
                    style = IntermTheme.typography.hDisplay.copy(
                        fontSize = 42.sp,
                        fontWeight = FontWeight.W400,
                        lineHeight = 42.sp,
                    ),
                    color = c.primary,
                )
                Text(
                    "h",
                    style = IntermTheme.typography.hDisplay.copy(fontSize = 22.sp),
                    color = c.primary2,
                    modifier = Modifier.padding(start = 2.dp, bottom = 6.dp),
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    mm.toString().padStart(2, '0'),
                    style = IntermTheme.typography.hDisplay.copy(
                        fontSize = 42.sp,
                        fontWeight = FontWeight.W400,
                        lineHeight = 42.sp,
                    ),
                    color = c.primary,
                )
                Text(
                    "m",
                    style = IntermTheme.typography.hDisplay.copy(fontSize = 22.sp),
                    color = c.primary2,
                    modifier = Modifier.padding(start = 2.dp, bottom = 6.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(c.primarySoft)
                    .padding(horizontal = 11.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (goalMet) {
                    Icon(
                        IntermIcons.Check,
                        contentDescription = null,
                        tint = c.primary,
                        modifier = Modifier.size(14.dp),
                    )
                }
                Text(
                    "${summary.planLabel} · ${if (goalMet) "Goal reached" else "$pct% of goal"}",
                    style = IntermTheme.typography.caption.copy(fontSize = 12.sp, fontWeight = FontWeight.W500),
                    color = c.primary,
                )
            }
        }

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

            Spacer(Modifier.height(16.dp))
            BrandFooter()

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

@Composable
private fun BrandFooter() {
    val c = IntermTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            IntermIcons.Home,
            contentDescription = null,
            tint = c.primary.copy(alpha = 0.55f),
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.size(6.dp))
        Text(
            "Fast",
            style = IntermTheme.typography.body.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.W600,
            ),
            color = c.ink.copy(alpha = 0.55f),
        )
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
