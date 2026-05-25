package xyz.jishnu.health.ui.screens.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import xyz.jishnu.health.data.model.DayEntry
import xyz.jishnu.health.data.model.Plan
import xyz.jishnu.health.data.model.Units
import xyz.jishnu.health.domain.WeightMath
import xyz.jishnu.health.ui.components.BottomNav
import xyz.jishnu.health.ui.components.IntermIcons
import xyz.jishnu.health.ui.components.IntermTopBar
import xyz.jishnu.health.ui.components.NavTab
import xyz.jishnu.health.ui.theme.IntermTheme
import xyz.jishnu.health.vm.HistoryViewModel
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.min

@Composable
fun HistoryScreen(
    onNavigateTab: (NavTab) -> Unit,
    onOpenDay: (Long) -> Unit,
    vm: HistoryViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val c = IntermTheme.colors

    Box(modifier = Modifier.fillMaxSize().background(c.bg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            IntermTopBar(modifier = Modifier.statusBarsPadding(), title = "History")
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 24.dp),
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(c.card)
                            .padding(horizontal = 16.dp),
                    ) {
                        state.entries.forEachIndexed { idx, entry ->
                            HistoryRow(
                                entry = entry,
                                plan = state.plan,
                                units = state.units,
                                isLast = idx == state.entries.lastIndex,
                                onClick = { onOpenDay(entry.dayKey) },
                            )
                        }
                        if (state.entries.isEmpty()) {
                            Spacer(Modifier.height(24.dp))
                            Text("No history yet.", style = IntermTheme.typography.body, color = c.muted)
                            Spacer(Modifier.height(24.dp))
                        }
                    }
                }
            }
            BottomNav(
                active = NavTab.Progress,
                onChange = onNavigateTab,
                modifier = Modifier.navigationBarsPadding(),
            )
        }
    }
}

@Composable
private fun HistoryRow(
    entry: DayEntry,
    plan: Plan,
    units: Units,
    isLast: Boolean,
    onClick: () -> Unit,
) {
    val c = IntermTheme.colors
    val hours = entry.fastHours.toInt()
    val mins = ((entry.fastHours - hours) * 60).toInt()
    val hit = entry.fastHours >= plan.fastHours
    val weightLabel = entry.weight?.let { w ->
        val fw = WeightMath.fmtWeight(w.weightLb, units)
        "Weight ${fw.value} ${fw.unit}"
    } ?: "No weight logged"

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(
                modifier = Modifier.width(44.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    entry.date.format(DateTimeFormatter.ofPattern("EEE", Locale.getDefault())).uppercase(),
                    style = IntermTheme.typography.caption.copy(fontSize = 10.sp, letterSpacing = 0.06.em()),
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
                    GoalChip(hit = hit)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    weightLabel,
                    style = IntermTheme.typography.caption,
                    color = c.muted,
                )
            }
            Column(
                modifier = Modifier.width(70.dp),
                horizontalAlignment = Alignment.End,
            ) {
                HistoryBar(hours = entry.fastHours, goalH = plan.fastHours, hit = hit)
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
private fun GoalChip(hit: Boolean) {
    val c = IntermTheme.colors
    val bg = if (hit) c.primarySoft else c.accentSoft
    val fg = if (hit) c.primary else c.accent
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            if (hit) "GOAL" else "SHORT",
            style = IntermTheme.typography.caption.copy(fontSize = 10.sp, fontWeight = FontWeight.W600, letterSpacing = 0.06.em()),
            color = fg,
        )
    }
}

@Composable
private fun HistoryBar(hours: Double, goalH: Int, hit: Boolean) {
    val c = IntermTheme.colors
    val fillRatio = min(1.0, hours / 24.0).toFloat()
    val goalRatio = (goalH / 24f).coerceIn(0f, 1f)
    val fg = if (hit) c.primary else c.accent
    Canvas(modifier = Modifier.size(width = 64.dp, height = 12.dp)) {
        val w = size.width
        val barTop = 3f
        val barHeight = 6f
        val radius = barHeight / 2f

        drawRoundRect(
            color = c.border2,
            topLeft = Offset(0f, barTop),
            size = Size(w, barHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius),
        )
        if (fillRatio > 0f) {
            drawRoundRect(
                color = fg,
                topLeft = Offset(0f, barTop),
                size = Size(w * fillRatio, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius),
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

private fun Number.em() = androidx.compose.ui.unit.TextUnit(this.toFloat(), androidx.compose.ui.unit.TextUnitType.Em)
