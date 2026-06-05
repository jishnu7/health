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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.jishnu.health.data.constants.Stages
import xyz.jishnu.health.domain.StageCalculator
import xyz.jishnu.health.ui.theme.IntermTheme
import xyz.jishnu.health.ui.theme.stageColors

private data class StageSegment(val id: String, val start: Float, val end: Float)

private fun stageSegments(): List<StageSegment> {
    val pool = Stages.all.filter { it.startHour < 24 }
    return pool.mapIndexed { idx, s ->
        val end = pool.getOrNull(idx + 1)?.startHour?.toFloat() ?: 24f
        StageSegment(id = s.id, start = s.startHour.toFloat(), end = end)
    }
}

private fun fmtHM(h: Double): String {
    val total = maxOf(0.0, h)
    val hh = total.toInt()
    val mm = ((total - hh) * 60).toInt()
    val (h2, m2) = if (mm >= 60) (hh + 1) to (mm - 60) else hh to mm
    return "${h2}h ${m2.toString().padStart(2, '0')}m"
}

/**
 * The active metabolic stage as a focus card — tinted hero panel with the
 * stage message, a 24h stage ribbon underneath, and the countdown to the next
 * stage. Driven entirely by [Stages.all] (the 8-stage metabolic model); the
 * older 4-phase "energy" lens has been retired.
 */
@Composable
fun MetabolicStageCard(
    elapsedHours: Double,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val c = IntermTheme.colors
    val stages = Stages.all
    val active = StageCalculator.stageFor(elapsedHours, stages)
    val next = stages.firstOrNull { it.startHour > elapsedHours }
    val palette = stageColors()
    val activeColor = palette[active.id] ?: c.primary
    val segs = stageSegments()
    val activeMessage = active.message

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(c.card)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(lerp(c.card, activeColor, 0.12f))
                .padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(activeColor),
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        active.name.uppercase(),
                        style = IntermTheme.typography.hEyebrow,
                        color = c.ink2,
                    )
                }
                if (onClick != null) {
                    Icon(IntermIcons.Chevron, contentDescription = null, tint = c.muted)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                activeMessage,
                style = IntermTheme.typography.headerTitle.copy(
                    fontSize = 21.sp,
                    fontWeight = FontWeight.W500,
                    lineHeight = 25.sp,
                ),
                color = c.ink,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(8.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                segs.forEach { seg ->
                    val segColor = palette[seg.id] ?: c.primary
                    val span = (seg.end - seg.start).coerceAtLeast(0.001f)
                    val weight = span
                    val fill = ((elapsedHours - seg.start) / span).coerceIn(0.0, 1.0).toFloat()
                    Box(
                        modifier = Modifier
                            .weight(weight)
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                if (next != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Next: ",
                            style = IntermTheme.typography.caption,
                            color = c.muted,
                        )
                        Text(
                            next.name,
                            style = IntermTheme.typography.caption.copy(fontWeight = FontWeight.W500),
                            color = c.ink2,
                        )
                    }
                    Text(
                        "in ${fmtHM(next.startHour - elapsedHours)}",
                        style = IntermTheme.typography.mono.copy(fontSize = 13.sp, fontWeight = FontWeight.W500),
                        color = activeColor,
                    )
                } else {
                    Text(
                        "Deepest stage reached",
                        style = IntermTheme.typography.caption.copy(fontWeight = FontWeight.W500),
                        color = c.ink2,
                    )
                }
            }
        }
    }
}
