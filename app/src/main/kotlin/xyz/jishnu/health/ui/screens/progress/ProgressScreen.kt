package xyz.jishnu.health.ui.screens.progress

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import xyz.jishnu.health.data.model.RangeOption
import xyz.jishnu.health.domain.WeightMath
import xyz.jishnu.health.ui.components.BottomNav
import xyz.jishnu.health.ui.components.HistoryRow
import xyz.jishnu.health.ui.components.IntermCard
import xyz.jishnu.health.ui.components.IntermIcons
import xyz.jishnu.health.ui.components.IntermTopBar
import xyz.jishnu.health.ui.components.NavTab
import xyz.jishnu.health.ui.theme.IntermTheme
import xyz.jishnu.health.vm.ProgressViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

@Composable
fun ProgressScreen(
    onNavigateTab: (NavTab) -> Unit,
    onOpenDay: (Long) -> Unit,
    vm: ProgressViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val pickerOpen by vm.pickerOpenFlow.collectAsStateWithLifecycle()
    val c = IntermTheme.colors

    Box(modifier = Modifier.fillMaxSize().background(c.bg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            IntermTopBar(modifier = Modifier.statusBarsPadding(), title = "Progress")
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 4.dp),
            ) {
                RangeChip(label = rangeLabel(state.range), open = pickerOpen, onClick = vm::togglePicker)

                if (pickerOpen) {
                    Spacer(Modifier.height(12.dp))
                    RangePickerCard(
                        selected = state.range,
                        onSelect = { vm.setRange(it); vm.setPickerOpen(false) },
                        onCustom = { from, to -> vm.setRange(RangeOption.Custom(from, to)) },
                        onDone = { vm.setPickerOpen(false) },
                    )
                } else {
                    Spacer(Modifier.height(18.dp))
                }

                IntermCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 14.dp)) {
                    LegendRow(unitsLabel = if (state.units == xyz.jishnu.health.data.model.Units.Metric) "kg" else "lb")
                    Spacer(Modifier.height(8.dp))
                    FastChart(entries = state.entries, units = state.units)
                }

                Spacer(Modifier.height(14.dp))
                SummaryGrid(state)

                Spacer(Modifier.height(22.dp))
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                    Text("HISTORY", style = IntermTheme.typography.hEyebrow, color = c.muted)
                    Text(
                        "${state.entries.size} ${if (state.entries.size == 1) "entry" else "entries"}",
                        style = IntermTheme.typography.caption,
                        color = c.muted,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(c.card)
                        .padding(horizontal = 16.dp),
                ) {
                    val rows = state.entries.asReversed()
                    rows.forEachIndexed { idx, entry ->
                        HistoryRow(
                            entry = entry,
                            plan = state.plan,
                            units = state.units,
                            isLast = idx == rows.lastIndex,
                            onClick = { onOpenDay(entry.dayKey) },
                        )
                    }
                    if (rows.isEmpty()) {
                        Spacer(Modifier.height(24.dp))
                        Text("No entries in this range.", style = IntermTheme.typography.body, color = c.muted)
                        Spacer(Modifier.height(24.dp))
                    }
                }
                Spacer(Modifier.height(20.dp))
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
private fun RangeChip(label: String, open: Boolean, onClick: () -> Unit) {
    val c = IntermTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(c.border2)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(IntermIcons.Calendar, contentDescription = null, tint = c.ink, modifier = Modifier.size(14.dp))
            Text(label, style = IntermTheme.typography.body.copy(fontSize = 13.sp, fontWeight = FontWeight.W500), color = c.ink)
            Icon(
                IntermIcons.ChevronDown,
                contentDescription = null,
                tint = c.ink.copy(alpha = 0.6f),
                modifier = Modifier.size(12.dp).rotate(if (open) 180f else 0f),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun RangePickerCard(
    selected: RangeOption,
    onSelect: (RangeOption) -> Unit,
    onCustom: (LocalDate, LocalDate) -> Unit,
    onDone: () -> Unit,
) {
    val c = IntermTheme.colors
    var openFromPicker by remember { mutableStateOf(false) }
    var openToPicker by remember { mutableStateOf(false) }

    val custom = selected as? RangeOption.Custom
    val today = remember { LocalDate.now() }
    val initialFrom = custom?.from ?: today.minusDays(13)
    val initialTo = custom?.to ?: today
    var fromDate by remember { mutableStateOf(initialFrom) }
    var toDate by remember { mutableStateOf(initialTo) }

    IntermCard(modifier = Modifier.fillMaxWidth().animateContentSize()) {
        Text("QUICK SELECT", style = IntermTheme.typography.hEyebrow, color = c.muted)
        Spacer(Modifier.height(10.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            RangeOption.quickOptions.forEach { opt ->
                val on = selected.id == opt.id
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (on) c.primary else c.border2)
                        .clickable { onSelect(opt) }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                ) {
                    Text(
                        opt.label,
                        style = IntermTheme.typography.body.copy(fontSize = 12.sp, fontWeight = FontWeight.W500),
                        color = if (on) c.surface else c.ink2,
                    )
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(c.border))
        Spacer(Modifier.height(14.dp))
        Text("CUSTOM RANGE", style = IntermTheme.typography.hEyebrow, color = c.muted)
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DateField(label = "FROM", value = fromDate, modifier = Modifier.weight(1f)) { openFromPicker = true }
            Text("→", style = IntermTheme.typography.body, color = c.muted)
            DateField(label = "TO", value = toDate, modifier = Modifier.weight(1f)) { openToPicker = true }
        }
        Spacer(Modifier.height(14.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(c.primary)
                    .clickable(onClick = onDone)
                    .padding(horizontal = 18.dp, vertical = 8.dp),
            ) {
                Text("Done", style = IntermTheme.typography.body.copy(fontSize = 12.sp, fontWeight = FontWeight.W500), color = c.surface)
            }
        }
    }

    if (openFromPicker) {
        DateDialog(
            initial = fromDate,
            onDismiss = { openFromPicker = false },
            onConfirm = { picked ->
                openFromPicker = false
                fromDate = picked
                onCustom(picked, toDate)
            },
        )
    }
    if (openToPicker) {
        DateDialog(
            initial = toDate,
            onDismiss = { openToPicker = false },
            onConfirm = { picked ->
                openToPicker = false
                toDate = picked
                onCustom(fromDate, picked)
            },
        )
    }
}

@Composable
private fun DateField(label: String, value: LocalDate, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = IntermTheme.colors
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(c.border2)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label, style = IntermTheme.typography.caption.copy(fontSize = 10.sp, letterSpacing = androidx.compose.ui.unit.TextUnit(0.08f, androidx.compose.ui.unit.TextUnitType.Em)), color = c.muted)
        Text(
            value.format(DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault())),
            style = IntermTheme.typography.mono.copy(fontSize = 13.sp, fontWeight = FontWeight.W500),
            color = c.ink,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateDialog(initial: LocalDate, onDismiss: () -> Unit, onConfirm: (LocalDate) -> Unit) {
    val zone = remember { ZoneId.systemDefault() }
    val initMs = initial.atStartOfDay(zone).toInstant().toEpochMilli()
    val state = rememberDatePickerState(initialSelectedDateMillis = initMs)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { ms ->
                    val date = Instant.ofEpochMilli(ms).atZone(ZoneId.of("UTC")).toLocalDate()
                    onConfirm(date)
                }
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    ) {
        DatePicker(state = state)
    }
}

@Composable
private fun LegendRow(unitsLabel: String) {
    val c = IntermTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        LegendItem(color = c.primary, label = "Weight ($unitsLabel)")
        LegendItem(color = c.accent, label = "Fast (h)")
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    val c = IntermTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(modifier = Modifier.size(width = 10.dp, height = 2.dp).clip(RoundedCornerShape(1.dp)).background(color))
        Text(label, style = IntermTheme.typography.caption, color = c.ink2)
    }
}

@Composable
private fun SummaryGrid(state: xyz.jishnu.health.vm.ProgressUiState) {
    val c = IntermTheme.colors
    val avgFastFmt = "%.1f".format(Locale.US, state.avgFastHours)
    val totalFast = state.totalFastHours.toInt()
    val deltaW = WeightMath.fmtWeight(abs(state.weightChangeLb), state.units)
    val startW = state.weightStartLb?.let { WeightMath.fmtWeight(it, state.units) }
    val endW = state.weightEndLb?.let { WeightMath.fmtWeight(it, state.units) }
    val deltaColor = if (state.weightChangeLb < 0) c.primary else if (state.weightChangeLb > 0) c.accent else c.ink

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryCard(modifier = Modifier.weight(1f)) {
                StatHeader("Avg fast")
                StatValue(value = avgFastFmt, unit = "h / day")
                StatSub("${state.dayCount}-day average")
            }
            SummaryCard(modifier = Modifier.weight(1f)) {
                StatHeader("Weight change")
                if (state.weightStartLb != null && state.weightEndLb != null) {
                    val sign = if (state.weightChangeLb < 0) "−" else if (state.weightChangeLb > 0) "+" else ""
                    StatValue(value = "$sign${deltaW.value}", unit = deltaW.unit, valueColor = deltaColor)
                    StatSub("${startW?.value} → ${endW?.value} ${endW?.unit}")
                } else {
                    StatValue(value = "—", unit = deltaW.unit)
                    StatSub("Need 2+ weigh-ins")
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryCard(modifier = Modifier.weight(1f)) {
                StatHeader("Total fasted")
                StatValue(value = totalFast.toString(), unit = "hours")
                val sinceText = state.entries.firstOrNull()?.date
                    ?.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault()))
                    ?: "—"
                StatSub("Since $sinceText")
            }
            SummaryCard(modifier = Modifier.weight(1f)) {
                StatHeader("Streak")
                StatValue(value = state.streakDays.toString(), unit = "days")
                StatSub(if (state.streakDays > 0) "Hit goal every day" else "No streak yet")
            }
        }
    }
}

@Composable
private fun SummaryCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    IntermCard(
        modifier = modifier,
        contentPadding = PaddingValues(14.dp),
        content = { Column(verticalArrangement = Arrangement.spacedBy(2.dp)) { content() } },
    )
}

@Composable
private fun StatHeader(text: String) {
    val c = IntermTheme.colors
    Text(text, style = IntermTheme.typography.caption, color = c.muted)
}

@Composable
private fun StatValue(value: String, unit: String, valueColor: Color = IntermTheme.colors.ink) {
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 4.dp)) {
        Text(value, style = IntermTheme.typography.mono.copy(fontSize = 24.sp, fontWeight = FontWeight.W500), color = valueColor)
        Text(unit, style = IntermTheme.typography.caption, color = IntermTheme.colors.muted, modifier = Modifier.padding(bottom = 4.dp))
    }
}

@Composable
private fun StatSub(text: String) {
    Text(text, style = IntermTheme.typography.caption, color = IntermTheme.colors.muted, modifier = Modifier.padding(top = 2.dp))
}

private fun rangeLabel(range: RangeOption): String = when (range) {
    is RangeOption.Custom -> {
        val fmt = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
        "${range.from.format(fmt)} – ${range.to.format(fmt)}"
    }
    else -> range.label
}
