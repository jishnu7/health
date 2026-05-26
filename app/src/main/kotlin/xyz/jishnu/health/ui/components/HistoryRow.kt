package xyz.jishnu.health.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.jishnu.health.data.model.DayEntry
import xyz.jishnu.health.data.model.FastStatus
import xyz.jishnu.health.data.model.Plan
import xyz.jishnu.health.data.model.Units
import xyz.jishnu.health.data.model.status
import xyz.jishnu.health.domain.WeightMath
import xyz.jishnu.health.ui.theme.IntermTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.min

@Composable
fun HistoryRow(
    entry: DayEntry,
    plan: Plan,
    units: Units,
    isLast: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = IntermTheme.colors
    val hours = entry.fastHours.toInt()
    val mins = ((entry.fastHours - hours) * 60).toInt()
    val status = entry.status(plan.fastHours)
    val startTimeLabel = entry.session?.let { s ->
        val lt = Instant.ofEpochMilli(s.startMs).atZone(ZoneId.systemDefault()).toLocalTime()
        DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()).format(lt)
    }
    val weightText = entry.weight?.let { w ->
        val fw = WeightMath.fmtWeight(w.weightLb, units)
        "Weight ${fw.value} ${fw.unit}"
    }
    val subLabel = when {
        startTimeLabel != null && weightText != null -> "From $startTimeLabel · $weightText"
        startTimeLabel != null -> "From $startTimeLabel"
        weightText != null -> weightText
        else -> "No data"
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(modifier = Modifier.width(44.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    entry.date.format(DateTimeFormatter.ofPattern("EEE", Locale.getDefault())).uppercase(),
                    style = IntermTheme.typography.caption.copy(fontSize = 10.sp, letterSpacing = em(0.06)),
                    color = c.muted,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    entry.date.dayOfMonth.toString(),
                    style = IntermTheme.typography.mono.copy(fontSize = 19.sp, fontWeight = FontWeight.W500),
                    color = c.ink,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "${hours}h ${mins}m",
                        style = IntermTheme.typography.body.copy(fontWeight = FontWeight.W500),
                        color = c.ink,
                    )
                    GoalChip(status = status)
                }
                Spacer(Modifier.height(4.dp))
                Text(subLabel, style = IntermTheme.typography.caption, color = c.muted)
            }
            Column(modifier = Modifier.width(70.dp), horizontalAlignment = Alignment.End) {
                HistoryBar(hours = entry.fastHours, goalH = plan.fastHours, status = status)
                Spacer(Modifier.height(4.dp))
                Text(
                    "goal ${plan.fastHours}h",
                    style = IntermTheme.typography.mono.copy(fontSize = 10.sp),
                    color = c.muted,
                )
            }
            Icon(IntermIcons.Chevron, contentDescription = null, tint = c.subtle)
        }
        if (!isLast) Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(c.border))
    }
}

@Composable
fun GoalChip(status: FastStatus, modifier: Modifier = Modifier) {
    val c = IntermTheme.colors
    val (label, bg, fg) = when (status) {
        FastStatus.Goal -> Triple("GOAL", c.primarySoft, c.primary)
        FastStatus.Short -> Triple("SHORT", c.accentSoft, c.accent)
        FastStatus.Ongoing -> Triple("ONGOING", c.border2, c.ink2)
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            label,
            style = IntermTheme.typography.caption.copy(fontSize = 10.sp, fontWeight = FontWeight.W600, letterSpacing = em(0.06)),
            color = fg,
        )
    }
}

@Composable
fun HistoryBar(hours: Double, goalH: Int, status: FastStatus, modifier: Modifier = Modifier) {
    val c = IntermTheme.colors
    val fillRatio = min(1.0, hours / 24.0).toFloat()
    val goalRatio = (goalH / 24f).coerceIn(0f, 1f)
    val fg = when (status) {
        FastStatus.Goal -> c.primary
        FastStatus.Short -> c.accent
        FastStatus.Ongoing -> c.ink2
    }
    Canvas(modifier = modifier.size(width = 64.dp, height = 12.dp)) {
        val w = size.width
        val barTop = 3f
        val barHeight = 6f
        val radius = barHeight / 2f
        drawRoundRect(
            color = c.border2,
            topLeft = Offset(0f, barTop),
            size = Size(w, barHeight),
            cornerRadius = CornerRadius(radius, radius),
        )
        if (fillRatio > 0f) {
            drawRoundRect(
                color = fg,
                topLeft = Offset(0f, barTop),
                size = Size(w * fillRatio, barHeight),
                cornerRadius = CornerRadius(radius, radius),
            )
        }
        val tickX = (w * goalRatio).coerceIn(0f, w - 1f)
        drawRect(
            color = c.ink.copy(alpha = 0.5f),
            topLeft = Offset(tickX, 0f),
            size = Size(1.5f, size.height),
        )
    }
}

internal fun em(value: Double): TextUnit = TextUnit(value.toFloat(), TextUnitType.Em)
