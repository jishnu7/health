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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import xyz.jishnu.health.data.constants.Stages
import xyz.jishnu.health.data.local.FastingSessionEntity
import xyz.jishnu.health.data.model.FastStatus
import xyz.jishnu.health.domain.StageCalculator
import xyz.jishnu.health.domain.TimeMath
import xyz.jishnu.health.domain.WaterMath
import xyz.jishnu.health.domain.WeightMath
import xyz.jishnu.health.ui.components.GoalChip
import xyz.jishnu.health.ui.components.IntermButton
import xyz.jishnu.health.ui.components.IntermButtonVariant
import xyz.jishnu.health.ui.components.IntermCard
import xyz.jishnu.health.ui.components.IntermIcons
import xyz.jishnu.health.ui.components.IntermStageChip
import xyz.jishnu.health.ui.components.IntermTopBar
import xyz.jishnu.health.ui.components.ProgressRing
import xyz.jishnu.health.ui.components.StageDots
import xyz.jishnu.health.ui.components.TimeRow
import xyz.jishnu.health.ui.screens.water.WaterGlass
import xyz.jishnu.health.ui.theme.IntermTheme
import xyz.jishnu.health.vm.DayDetailViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

@Composable
fun DayDetailScreen(
    onBack: () -> Unit,
    onOpenSession: (Long) -> Unit = {},
    onResumed: () -> Unit = onBack,
    vm: DayDetailViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val c = IntermTheme.colors
    if (!state.loaded) return

    val durationHours = state.durationHours
    val dh = durationHours.toInt()
    val dm = ((durationHours - dh) * 60).toInt()
    val durationLabel = if (state.hasSession) "${dh}h ${dm.toString().padStart(2, '0')}m" else "—"
    // No badge / no fill until the user has actually logged something.
    val status: xyz.jishnu.health.data.model.FastStatus? = when {
        !state.hasSession -> null
        state.isOngoing -> xyz.jishnu.health.data.model.FastStatus.Ongoing
        state.goalMet -> xyz.jishnu.health.data.model.FastStatus.Goal
        else -> xyz.jishnu.health.data.model.FastStatus.Short
    }
    val wf = WeightMath.fmtWeight(state.weightKg, state.units)
    val prevDeltaKg = state.previousWeightKg?.let { state.weightKg - it } ?: 0.0
    val prevDeltaW = WeightMath.fmtWeight(abs(prevDeltaKg), state.units)
    val weightStepKg = WeightMath.deltaToKg(0.1, state.units)

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
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                FastDisplay(
                    state = state,
                    status = status,
                    onSetStart = vm::setStart,
                    onSetEnd = vm::setEnd,
                    onResume = { vm.resumeFast(onResumed) },
                )

                SectionLabel("Weight")
                IntermCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StepperButton(IntermIcons.Minus, size = 40) { vm.bumpWeightKg(-weightStepKg) }
                            Spacer(Modifier.width(14.dp))
                            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.width(130.dp), horizontalArrangement = Arrangement.Center) {
                                Text(wf.value, style = IntermTheme.typography.hDisplay.copy(fontSize = 40.sp), color = c.ink)
                                Spacer(Modifier.width(6.dp))
                                Text(wf.unit, style = IntermTheme.typography.body.copy(fontSize = 16.sp), color = c.muted, modifier = Modifier.padding(bottom = 6.dp))
                            }
                            Spacer(Modifier.width(14.dp))
                            StepperButton(IntermIcons.Plus, size = 40) { vm.bumpWeightKg(weightStepKg) }
                        }
                        if (state.previousWeightKg != null) {
                            Spacer(Modifier.height(8.dp))
                            val deltaColor = if (prevDeltaKg < 0) c.primary else if (prevDeltaKg > 0) c.accent else c.muted
                            val deltaSign = if (prevDeltaKg < 0) "−" else if (prevDeltaKg > 0) "+" else ""
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

                SectionLabel("Water")
                DayWaterCard(
                    ml = state.waterMl,
                    goalMl = state.waterGoalMl,
                    units = state.units,
                )

                if (state.daySessions.size > 1) {
                    SectionLabel("Fasts on this day")
                    SettingsCard {
                        state.daySessions.forEachIndexed { idx, s ->
                            DaySessionRow(
                                session = s,
                                isSelected = s.id == state.sessionId,
                                goalH = state.goalHours,
                                nowMs = state.nowMs,
                                onClick = { onOpenSession(s.id) },
                                showDivider = idx != state.daySessions.lastIndex,
                            )
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
private fun DayWaterCard(ml: Int, goalMl: Int, units: xyz.jishnu.health.data.model.Units) {
    val c = IntermTheme.colors
    val total = WaterMath.fmtVolume(ml, units)
    val goal = WaterMath.fmtVolume(goalMl, units)
    val progress = if (goalMl > 0) (ml.toFloat() / goalMl.toFloat()).coerceIn(0f, 1f) else 0f
    val pct = (progress * 100).toInt()
    val hit = goalMl > 0 && ml >= goalMl
    IntermCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            WaterGlass(progress = progress, size = 88.dp)
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        total.value,
                        style = IntermTheme.typography.hDisplay.copy(fontSize = 24.sp, fontWeight = FontWeight.W500),
                        color = c.ink,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        total.unit,
                        style = IntermTheme.typography.body.copy(fontSize = 13.sp),
                        color = c.muted,
                        modifier = Modifier.padding(bottom = 2.dp),
                    )
                    if (hit) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(c.primarySoft)
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(
                                "GOAL",
                                style = IntermTheme.typography.caption.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.W600,
                                    letterSpacing = 0.06.em(),
                                ),
                                color = c.primary,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    "of ${goal.value} ${goal.unit} · $pct%",
                    style = IntermTheme.typography.mono.copy(fontSize = 12.sp),
                    color = c.muted,
                )
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(c.border2),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(c.primary),
                    )
                }
            }
        }
    }
}

/**
 * Mirrors the Home screen's active-fast layout — stage chip, progress ring with
 * elapsed time, and time markers — but driven by the day's recorded (or
 * in-progress) session. The Started/Ended markers double as inline editors,
 * tapping either opens a [TimePickerDialog] that writes through
 * [onSetStart] / [onSetEnd].
 */
@Composable
private fun FastDisplay(
    state: xyz.jishnu.health.vm.DayDetailUiState,
    status: xyz.jishnu.health.data.model.FastStatus?,
    onSetStart: (String) -> Unit,
    onSetEnd: (String) -> Unit,
    onResume: () -> Unit,
) {
    val c = IntermTheme.colors
    val stages = Stages.all
    val durationHours = state.durationHours
    val stage = StageCalculator.stageFor(durationHours, stages)
    val stageIdx = stages.indexOf(stage).coerceAtLeast(0)
    val progress = if (state.goalHours > 0) (durationHours / state.goalHours).coerceIn(0.0, 1.0).toFloat() else 0f

    // For ongoing fasts the third marker shows the projected goal end time
    // (start + goalHours), wrapped within the 24-hour clock. It's read-only —
    // the user can't edit when an in-progress fast will "end" except by ending
    // it on Home.
    val goalEndTime = state.startTime?.let { TimeMath.addHoursToTime(it, state.goalHours.toDouble()) }
    val endMarkerValue = when {
        state.isOngoing -> goalEndTime ?: "—"
        else -> state.displayedEndTime ?: "—"
    }

    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    Spacer(Modifier.height(20.dp))

    ProgressRing(
        progress = progress,
        size = 220.dp,
        stroke = 10.dp,
        dashed = !state.hasSession,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (state.hasSession) {
                val elapsedMs = (durationHours * 3_600_000.0).toLong()
                val d = TimeMath.fmtDuration(elapsedMs)
                Text(
                    if (state.isOngoing) "ELAPSED" else "DURATION",
                    style = IntermTheme.typography.hEyebrow,
                    color = c.muted,
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(d.hh, style = IntermTheme.typography.hDisplay.copy(fontSize = 44.sp), color = c.ink)
                    Text(":", style = IntermTheme.typography.hDisplay.copy(fontSize = 44.sp), color = c.muted)
                    Text(d.mm, style = IntermTheme.typography.hDisplay.copy(fontSize = 44.sp), color = c.ink)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "${d.ss}s · goal ${state.goalHours}h",
                    style = IntermTheme.typography.caption,
                    color = c.muted,
                )
                if (state.canResume) {
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(c.primarySoft)
                            .clickable(onClick = onResume)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(IntermIcons.Play, contentDescription = null, tint = c.primary, modifier = Modifier.size(12.dp))
                        Text(
                            "Resume",
                            style = IntermTheme.typography.caption.copy(fontSize = 11.sp, fontWeight = FontWeight.W600),
                            color = c.primary,
                        )
                    }
                }
            } else {
                Text("GOAL", style = IntermTheme.typography.hEyebrow, color = c.muted)
                Spacer(Modifier.height(6.dp))
                Text(
                    "${state.goalHours}h",
                    style = IntermTheme.typography.hDisplay.copy(fontSize = 44.sp),
                    color = c.ink,
                )
            }
        }
    }

    Spacer(Modifier.height(20.dp))

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        FastMarker(
            label = "Started",
            value = state.startTime ?: "—",
            align = Alignment.Start,
            onClick = { showStartPicker = true },
        )
        val middleLabel: String
        val middleValue: String
        if (state.isOngoing) {
            val remainingH = (state.goalHours - durationHours).coerceAtLeast(0.0)
            val rh = remainingH.toInt()
            val rm = ((remainingH - rh) * 60).toInt()
            middleLabel = "Remaining"
            middleValue = "${rh}h ${rm.toString().padStart(2, '0')}m"
        } else if (state.hasSession) {
            val ih = durationHours.toInt()
            val im = ((durationHours - ih) * 60).toInt()
            middleLabel = "Duration"
            middleValue = "${ih}h ${im.toString().padStart(2, '0')}m"
        } else {
            middleLabel = "Duration"
            middleValue = "—"
        }
        FastMarker(label = middleLabel, value = middleValue, align = Alignment.CenterHorizontally)
        FastMarker(
            label = if (state.isOngoing) "Goal" else "Ended",
            value = endMarkerValue,
            align = Alignment.End,
            // Goal end time is derived from start + goalHours while a fast is
            // running; only "Ended" (completed fasts) is editable.
            onClick = if (state.isOngoing) null else ({ showEndPicker = true }),
        )
    }

    Spacer(Modifier.height(18.dp))

    if (state.hasSession) {
        xyz.jishnu.health.ui.components.EnergyBar(
            elapsedHours = durationHours,
            modifier = Modifier.fillMaxWidth(),
            compact = true,
        )
    }

    Spacer(Modifier.height(8.dp))

    if (showStartPicker) {
        xyz.jishnu.health.ui.components.TimePickerDialog(
            initial = state.startTime ?: "08:00",
            onDismiss = { showStartPicker = false },
            onConfirm = { showStartPicker = false; onSetStart(it) },
        )
    }
    if (showEndPicker) {
        xyz.jishnu.health.ui.components.TimePickerDialog(
            initial = state.displayedEndTime ?: state.startTime ?: "12:00",
            onDismiss = { showEndPicker = false },
            onConfirm = { showEndPicker = false; onSetEnd(it) },
        )
    }
}

@Composable
private fun FastMarker(
    label: String,
    value: String,
    align: Alignment.Horizontal,
    onClick: (() -> Unit)? = null,
) {
    val c = IntermTheme.colors
    val editable = onClick != null
    Column(
        horizontalAlignment = align,
        modifier = if (editable) {
            Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onClick!!)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        } else {
            Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        },
    ) {
        Text(label, style = IntermTheme.typography.caption, color = c.muted)
        Spacer(Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                value,
                style = IntermTheme.typography.mono.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.W500,
                    textDecoration = if (editable) androidx.compose.ui.text.style.TextDecoration.Underline else null,
                ),
                color = if (editable) c.primary else c.ink2,
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
private fun GoalBar(durationHours: Double, goalH: Int, status: xyz.jishnu.health.data.model.FastStatus?) {
    val c = IntermTheme.colors
    val fillRatio = if (status == null) 0f else (durationHours / 24.0).coerceIn(0.0, 1.0).toFloat()
    val goalRatio = (goalH / 24f).coerceIn(0f, 1f)
    val fg = when (status) {
        xyz.jishnu.health.data.model.FastStatus.Goal -> c.primary
        xyz.jishnu.health.data.model.FastStatus.Short -> c.accent
        xyz.jishnu.health.data.model.FastStatus.Ongoing -> c.ink2
        null -> c.subtle
    }
    // Stage boundaries (skip 0h start and the 24h endpoint).
    val stageBoundaryHours = xyz.jishnu.health.data.constants.Stages.all
        .map { it.startHour }
        .filter { it in 1..23 }
    Box(modifier = Modifier.fillMaxWidth().height(36.dp)) {
        Canvas(modifier = Modifier.fillMaxWidth().height(36.dp)) {
            val w = size.width
            val barTop = 24f
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
            // Stage dividers — contrast against whatever's underneath.
            val fillX = w * fillRatio
            val dividerWidth = 3f
            val dividerOverhang = 2f
            for (h in stageBoundaryHours) {
                val x = (h / 24f) * w
                val onFill = x <= fillX
                val dividerColor = if (onFill) {
                    c.surface.copy(alpha = 0.85f)
                } else {
                    c.ink.copy(alpha = 0.45f)
                }
                drawRect(
                    color = dividerColor,
                    topLeft = Offset(x - dividerWidth / 2f, barTop - dividerOverhang),
                    size = Size(dividerWidth, barHeight + dividerOverhang * 2),
                )
            }
            // Goal flag — pole rising above the bar with a triangular pennant.
            val tickX = (w * goalRatio).coerceIn(0f, w - 1f)
            val poleWidth = 4f
            val poleTop = 2f
            val poleBottom = barTop + barHeight + 2f
            drawRect(
                color = c.ink,
                topLeft = Offset(tickX - poleWidth / 2f, poleTop),
                size = Size(poleWidth, poleBottom - poleTop),
            )
            val flagStart = tickX + poleWidth / 2f
            val flagWidth = 22f
            val flagHeight = 18f
            val flagPath = androidx.compose.ui.graphics.Path().apply {
                moveTo(flagStart, poleTop)
                lineTo(flagStart + flagWidth, poleTop + flagHeight / 2f)
                lineTo(flagStart, poleTop + flagHeight)
                close()
            }
            drawPath(flagPath, color = c.primary)
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

@Composable
private fun DaySessionRow(
    session: FastingSessionEntity,
    isSelected: Boolean,
    goalH: Int,
    nowMs: Long,
    onClick: () -> Unit,
    showDivider: Boolean,
) {
    val c = IntermTheme.colors
    val endMs = session.endMs ?: nowMs
    val durationMs = (endMs - session.startMs).coerceAtLeast(0L)
    val durationH = durationMs / 3_600_000.0
    val h = durationH.toInt()
    val m = ((durationH - h) * 60).toInt()
    val status = when {
        session.endMs == null -> FastStatus.Ongoing
        durationH >= goalH -> FastStatus.Goal
        else -> FastStatus.Short
    }
    val startTimeText = run {
        val lt = Instant.ofEpochMilli(session.startMs).atZone(ZoneId.systemDefault()).toLocalTime()
        DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()).format(lt)
    }
    val endTimeText = session.endMs?.let { e ->
        val lt = Instant.ofEpochMilli(e).atZone(ZoneId.systemDefault()).toLocalTime()
        DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()).format(lt)
    }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier.size(8.dp).clip(CircleShape)
                    .background(if (isSelected) c.primary else Color.Transparent),
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "${h}h ${m}m",
                        style = IntermTheme.typography.body.copy(fontWeight = FontWeight.W500),
                        color = c.ink,
                    )
                    GoalChip(status = status)
                }
                Spacer(Modifier.height(2.dp))
                val rangeText = if (endTimeText != null) "$startTimeText – $endTimeText" else "From $startTimeText"
                Text(rangeText, style = IntermTheme.typography.caption, color = c.muted)
            }
            Icon(IntermIcons.Chevron, contentDescription = null, tint = c.subtle)
        }
        if (showDivider) Box(Modifier.fillMaxWidth().height(1.dp).background(c.border))
    }
}
