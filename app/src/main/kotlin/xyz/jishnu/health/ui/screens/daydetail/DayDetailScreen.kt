package xyz.jishnu.health.ui.screens.daydetail

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import xyz.jishnu.health.domain.WeightMath
import xyz.jishnu.health.ui.components.IntermButton
import xyz.jishnu.health.ui.components.IntermButtonVariant
import xyz.jishnu.health.ui.components.IntermCard
import xyz.jishnu.health.ui.components.IntermIcons
import xyz.jishnu.health.ui.components.IntermTopBar
import xyz.jishnu.health.ui.components.TimeRow
import xyz.jishnu.health.ui.theme.IntermTheme
import xyz.jishnu.health.vm.DayDetailViewModel
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

@Composable
fun DayDetailScreen(
    onBack: () -> Unit,
    vm: DayDetailViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val c = IntermTheme.colors
    if (!state.loaded) return

    val durationHours = state.durationHours
    val dh = durationHours.toInt()
    val dm = ((durationHours - dh) * 60).toInt()
    val status = when {
        state.isOngoing -> xyz.jishnu.health.data.model.FastStatus.Ongoing
        state.goalMet -> xyz.jishnu.health.data.model.FastStatus.Goal
        else -> xyz.jishnu.health.data.model.FastStatus.Short
    }
    val wf = WeightMath.fmtWeight(state.weightLb, state.units)
    val prevDelta = state.previousWeightLb?.let { state.weightLb - it } ?: 0.0
    val prevDeltaW = WeightMath.fmtWeight(abs(prevDelta), state.units)

    Box(modifier = Modifier.fillMaxSize().background(c.bg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            IntermTopBar(
                modifier = Modifier.statusBarsPadding(),
                title = state.date.format(DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault())),
                leading = {
                    Box(
                        modifier = Modifier.fillMaxSize().clickable(onClick = onBack),
                        contentAlignment = Alignment.Center,
                    ) { Icon(IntermIcons.Back, contentDescription = "Back", tint = c.ink2) }
                },
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 6.dp),
            ) {
                IntermCard(modifier = Modifier.fillMaxWidth()) {
                    Text("FASTING DURATION", style = IntermTheme.typography.hEyebrow, color = c.muted)
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(dh.toString(), style = IntermTheme.typography.hDisplay.copy(fontSize = 44.sp), color = c.ink)
                            Spacer(Modifier.width(2.dp))
                            Text("h", style = IntermTheme.typography.hDisplay.copy(fontSize = 22.sp), color = c.muted, modifier = Modifier.padding(bottom = 4.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(dm.toString().padStart(2, '0'), style = IntermTheme.typography.hDisplay.copy(fontSize = 44.sp), color = c.ink)
                            Spacer(Modifier.width(2.dp))
                            Text("m", style = IntermTheme.typography.hDisplay.copy(fontSize = 22.sp), color = c.muted, modifier = Modifier.padding(bottom = 4.dp))
                        }
                        GoalBadge(status = status)
                    }
                    Spacer(Modifier.height(14.dp))
                    GoalBar(durationHours = durationHours, goalH = state.goalHours, status = status)
                    Spacer(Modifier.height(20.dp))
                }

                SectionLabel("Fasting window")
                SettingsCard {
                    TimeRow(label = "Started", value = state.startTime, onValueChange = vm::setStart)
                    TimeRow(
                        label = "Ended",
                        value = state.displayedEndTime ?: "—",
                        sub = if (state.isOngoing) "Fast is still running" else null,
                        enabled = !state.isOngoing,
                        onValueChange = vm::setEnd,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Duration", style = IntermTheme.typography.body.copy(fontWeight = FontWeight.W500), color = c.ink)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                if (state.isOngoing) "Live, still running" else "Auto-calculated",
                                style = IntermTheme.typography.caption,
                                color = c.muted,
                            )
                        }
                        Text(
                            "${dh}h ${dm.toString().padStart(2, '0')}m",
                            style = IntermTheme.typography.mono.copy(fontSize = 14.sp, fontWeight = FontWeight.W500),
                            color = c.ink2,
                        )
                    }
                }

                SectionLabel("Weight")
                IntermCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StepperButton(IntermIcons.Minus, size = 40) { vm.bumpWeight(-0.1) }
                            Spacer(Modifier.width(14.dp))
                            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.width(130.dp), horizontalArrangement = Arrangement.Center) {
                                Text(wf.value, style = IntermTheme.typography.hDisplay.copy(fontSize = 40.sp), color = c.ink)
                                Spacer(Modifier.width(6.dp))
                                Text(wf.unit, style = IntermTheme.typography.body.copy(fontSize = 16.sp), color = c.muted, modifier = Modifier.padding(bottom = 6.dp))
                            }
                            Spacer(Modifier.width(14.dp))
                            StepperButton(IntermIcons.Plus, size = 40) { vm.bumpWeight(0.1) }
                        }
                        if (state.previousWeightLb != null) {
                            Spacer(Modifier.height(8.dp))
                            val deltaColor = if (prevDelta < 0) c.primary else if (prevDelta > 0) c.accent else c.muted
                            val deltaSign = if (prevDelta < 0) "−" else if (prevDelta > 0) "+" else ""
                            Row {
                                Text(
                                    "$deltaSign${prevDeltaW.value} ${prevDeltaW.unit}",
                                    style = IntermTheme.typography.mono.copy(fontSize = 12.sp),
                                    color = deltaColor,
                                )
                                Text(
                                    " vs. previous day",
                                    style = IntermTheme.typography.caption,
                                    color = c.muted,
                                )
                            }
                        }
                    }
                }

                SectionLabel("Notes")
                IntermCard(modifier = Modifier.fillMaxWidth()) {
                    BasicTextField(
                        value = state.notes,
                        onValueChange = vm::setNotes,
                        textStyle = LocalTextStyle.current.copy(color = c.ink, fontSize = 14.sp),
                        cursorBrush = SolidColor(c.primary),
                        modifier = Modifier.fillMaxWidth().height(72.dp),
                        decorationBox = { inner ->
                            if (state.notes.isEmpty()) {
                                Text("How did this day feel?", style = IntermTheme.typography.body, color = c.muted)
                            }
                            inner()
                        },
                    )
                }

                Spacer(Modifier.height(22.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    IntermButton(
                        onClick = { vm.delete(onBack) },
                        variant = IntermButtonVariant.Ghost,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Delete Entry", color = c.accent)
                    }
                    IntermButton(
                        onClick = { vm.save(onBack) },
                        variant = IntermButtonVariant.Primary,
                        modifier = Modifier.weight(1.4f),
                    ) {
                        Icon(IntermIcons.Check, contentDescription = null)
                        Text("Save")
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
        Spacer(Modifier.navigationBarsPadding())
    }
}

@Composable
private fun SectionLabel(text: String) {
    val c = IntermTheme.colors
    Text(
        text.uppercase(),
        style = IntermTheme.typography.hEyebrow,
        color = c.muted,
        modifier = Modifier.padding(start = 6.dp, top = 22.dp, bottom = 10.dp),
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    val c = IntermTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(c.card)
            .padding(horizontal = 16.dp),
    ) { content() }
}

@Composable
private fun GoalBadge(status: xyz.jishnu.health.data.model.FastStatus) {
    val c = IntermTheme.colors
    val (label, bg, fg) = when (status) {
        xyz.jishnu.health.data.model.FastStatus.Goal -> Triple("GOAL MET", c.primarySoft, c.primary)
        xyz.jishnu.health.data.model.FastStatus.Short -> Triple("SHORT", c.accentSoft, c.accent)
        xyz.jishnu.health.data.model.FastStatus.Ongoing -> Triple("ONGOING", c.border2, c.ink2)
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(
            label,
            style = IntermTheme.typography.caption.copy(fontSize = 11.sp, fontWeight = FontWeight.W600, letterSpacing = 0.08.em()),
            color = fg,
        )
    }
}

@Composable
private fun GoalBar(durationHours: Double, goalH: Int, status: xyz.jishnu.health.data.model.FastStatus) {
    val c = IntermTheme.colors
    val fillRatio = (durationHours / 24.0).coerceIn(0.0, 1.0).toFloat()
    val goalRatio = (goalH / 24f).coerceIn(0f, 1f)
    val fg = when (status) {
        xyz.jishnu.health.data.model.FastStatus.Goal -> c.primary
        xyz.jishnu.health.data.model.FastStatus.Short -> c.accent
        xyz.jishnu.health.data.model.FastStatus.Ongoing -> c.ink2
    }
    Box(modifier = Modifier.fillMaxWidth().height(18.dp)) {
        Canvas(modifier = Modifier.fillMaxWidth().height(18.dp)) {
            val w = size.width
            val barTop = 4f
            val barHeight = 8f
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
                topLeft = Offset(tickX - 0.75f, 2f),
                size = Size(1.5f, 14f),
            )
        }
    }
}

@Composable
private fun StepperButton(icon: androidx.compose.ui.graphics.vector.ImageVector, size: Int, onClick: () -> Unit) {
    val c = IntermTheme.colors
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(c.border2)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = c.ink2)
    }
}

private fun Number.em() = androidx.compose.ui.unit.TextUnit(this.toFloat(), androidx.compose.ui.unit.TextUnitType.Em)
