package xyz.jishnu.health.ui.screens.water

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import xyz.jishnu.health.data.local.WaterEntryEntity
import xyz.jishnu.health.data.model.Units
import xyz.jishnu.health.domain.TimeMath
import xyz.jishnu.health.domain.WaterMath
import xyz.jishnu.health.domain.WaterPreset
import xyz.jishnu.health.ui.components.BottomNav
import xyz.jishnu.health.ui.components.IntermCard
import xyz.jishnu.health.ui.components.IntermIcons
import xyz.jishnu.health.ui.components.IntermTopBar
import xyz.jishnu.health.ui.components.NavTab
import xyz.jishnu.health.ui.theme.IntermTheme
import xyz.jishnu.health.vm.WaterViewModel
import java.time.Instant
import java.time.ZoneId

@Composable
fun WaterScreen(
    onNavigateTab: (NavTab) -> Unit,
    vm: WaterViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val c = IntermTheme.colors
    val total = WaterMath.fmtVolume(state.totalMl, state.units)
    val goal = WaterMath.fmtVolume(state.goalMl, state.units)
    val remaining = WaterMath.fmtVolume(state.remainingMl, state.units)
    val pct = (state.progress * 100).toInt()

    var showCustom by remember { mutableStateOf(false) }
    var customText by remember { mutableStateOf(if (state.units == Units.Metric) "200" else "8") }

    Box(modifier = Modifier.fillMaxSize().background(c.bg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            IntermTopBar(modifier = Modifier.statusBarsPadding(), title = "Water")
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 6.dp),
            ) {
                Hero(
                    progress = state.progress,
                    totalValue = total.value,
                    totalUnit = total.unit,
                    goalValue = goal.value,
                    goalUnit = goal.unit,
                    pct = pct,
                    remainingValue = remaining.value,
                    remainingUnit = remaining.unit,
                    remainingMl = state.remainingMl,
                )

                Spacer(Modifier.height(22.dp))
                SectionLabel("Quick add")
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    WaterPreset.entries.forEach { preset ->
                        PresetButton(
                            preset = preset,
                            units = state.units,
                            onClick = { vm.addWater(preset.ml) },
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(999.dp))
                        .border(1.dp, c.border, RoundedCornerShape(999.dp))
                        .clickable { showCustom = !showCustom }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(IntermIcons.Plus, contentDescription = null, tint = c.ink2)
                        Text(
                            "Custom amount",
                            style = IntermTheme.typography.body.copy(fontSize = 13.sp, fontWeight = FontWeight.W500),
                            color = c.ink2,
                        )
                    }
                }

                if (showCustom) {
                    Spacer(Modifier.height(10.dp))
                    CustomAmountCard(
                        value = customText,
                        unitLabel = if (state.units == Units.Metric) "ml" else "fl oz",
                        onValueChange = { customText = it },
                        onAdd = {
                            val n = customText.toDoubleOrNull()
                            if (n != null && n > 0.0) {
                                vm.addWater(WaterMath.typedToMl(n, state.units))
                                showCustom = false
                            }
                        },
                    )
                }

                Spacer(Modifier.height(22.dp))
                SectionLabel("Today's log")
                Spacer(Modifier.height(8.dp))
                if (state.log.isEmpty()) {
                    IntermCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "No drinks logged yet. Tap a preset above.",
                            style = IntermTheme.typography.body,
                            color = c.muted,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(c.card)
                            .padding(horizontal = 16.dp),
                    ) {
                        val entries = state.log.asReversed()
                        entries.forEachIndexed { idx, entry ->
                            LogRow(
                                entry = entry,
                                units = state.units,
                                isLast = idx == entries.lastIndex,
                                onDelete = { vm.removeEntry(entry) },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
            BottomNav(
                active = NavTab.Water,
                onChange = onNavigateTab,
                modifier = Modifier.navigationBarsPadding(),
            )
        }
    }
}

@Composable
private fun Hero(
    progress: Float,
    totalValue: String,
    totalUnit: String,
    goalValue: String,
    goalUnit: String,
    pct: Int,
    remainingValue: String,
    remainingUnit: String,
    remainingMl: Int,
) {
    val c = IntermTheme.colors
    IntermCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            WaterGlass(progress = progress, size = 130.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text("TODAY", style = IntermTheme.typography.hEyebrow, color = c.muted)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        totalValue,
                        style = IntermTheme.typography.hDisplay.copy(fontSize = 38.sp),
                        color = c.ink,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        totalUnit,
                        style = IntermTheme.typography.caption.copy(fontSize = 14.sp),
                        color = c.muted,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "of $goalValue $goalUnit · $pct%",
                    style = IntermTheme.typography.mono.copy(fontSize = 12.sp),
                    color = c.muted,
                )
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(c.border2),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(c.primary),
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    if (remainingMl > 0) "$remainingValue $remainingUnit to go" else "Goal reached",
                    style = IntermTheme.typography.caption,
                    color = c.muted,
                )
            }
        }
    }
}

@Composable
fun WaterGlass(progress: Float, size: Dp) {
    val c = IntermTheme.colors
    val clamped = progress.coerceIn(0f, 1f)
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f
        val cupTopY = h * 0.15f
        val cupBotY = h * 0.92f
        val cupTopW = w * 0.55f
        val cupBotW = w * 0.45f
        val bottomCurve = h * 0.04f

        val glass = Path().apply {
            moveTo(cx - cupTopW / 2f, cupTopY)
            lineTo(cx - cupBotW / 2f, cupBotY)
            quadraticBezierTo(cx, cupBotY + bottomCurve, cx + cupBotW / 2f, cupBotY)
            lineTo(cx + cupTopW / 2f, cupTopY)
            close()
        }

        val fillH = (cupBotY - cupTopY) * clamped
        val fillTopY = cupBotY - fillH
        if (fillH > 0f) {
            clipPath(glass) {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            c.primary.copy(alpha = 0.55f),
                            c.primary.copy(alpha = 0.85f),
                        ),
                        startY = fillTopY,
                        endY = cupBotY + bottomCurve,
                    ),
                    topLeft = Offset(0f, fillTopY),
                    size = Size(w, (cupBotY + bottomCurve) - fillTopY),
                )
            }
        }

        // Outline
        drawPath(glass, color = c.border, style = Stroke(width = 1.5.dp.toPx()))
        // Rim
        drawLine(
            color = c.border,
            start = Offset(cx - cupTopW / 2f, cupTopY),
            end = Offset(cx + cupTopW / 2f, cupTopY),
            strokeWidth = 1.5.dp.toPx(),
        )
        // Tick marks at 25/50/75% on the left edge
        listOf(0.25f, 0.5f, 0.75f).forEach { frac ->
            val y = cupBotY - (cupBotY - cupTopY) * frac
            val widthAt = cupBotW + (cupTopW - cupBotW) * frac
            drawLine(
                color = c.subtle,
                start = Offset(cx - widthAt / 2f, y),
                end = Offset(cx - widthAt / 2f + 6f, y),
                strokeWidth = 1f,
            )
        }
    }
}

@Composable
private fun PresetButton(
    preset: WaterPreset,
    units: Units,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = IntermTheme.colors
    val v = WaterMath.fmtVolume(preset.ml, units)
    val bottleHeight = when (preset) {
        WaterPreset.Glass -> 22.dp
        WaterPreset.Cup -> 28.dp
        WaterPreset.Bottle -> 36.dp
        WaterPreset.Flask -> 42.dp
    }
    val bottleWidth = when (preset) {
        WaterPreset.Glass -> 14.dp
        WaterPreset.Cup -> 16.dp
        WaterPreset.Bottle -> 18.dp
        WaterPreset.Flask -> 20.dp
    }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(c.card)
            .border(1.dp, c.border, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BottleIcon(height = bottleHeight, widthDp = bottleWidth)
        Text(
            preset.label.uppercase(),
            style = IntermTheme.typography.caption.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.W600,
                letterSpacing = androidx.compose.ui.unit.TextUnit(0.04f, androidx.compose.ui.unit.TextUnitType.Em),
            ),
            color = c.ink2,
        )
        Text(
            "${v.value} ${v.unit}",
            style = IntermTheme.typography.mono.copy(fontSize = 11.sp),
            color = c.muted,
        )
    }
}

@Composable
private fun BottleIcon(height: Dp, widthDp: Dp) {
    val c = IntermTheme.colors
    Canvas(modifier = Modifier.size(width = 28.dp, height = height)) {
        val w = size.width
        val h = size.height
        val wPx = widthDp.toPx()
        val left = (w - wPx) / 2f
        val right = w - left
        val top = 4f
        val bottom = h - 2f
        val curve = h * 0.06f
        val bottle = Path().apply {
            moveTo(left, top)
            lineTo(left - 1f, bottom - curve)
            quadraticBezierTo(w / 2f, bottom + curve, right + 1f, bottom - curve)
            lineTo(right, top)
            close()
        }
        drawPath(bottle, color = c.primarySoft)
        drawPath(bottle, color = c.primary, style = Stroke(width = 1.2.dp.toPx()))
    }
}

@Composable
private fun CustomAmountCard(
    value: String,
    unitLabel: String,
    onValueChange: (String) -> Unit,
    onAdd: () -> Unit,
) {
    val c = IntermTheme.colors
    IntermCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(c.border2)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = LocalTextStyle.current.copy(
                        color = c.ink,
                        fontSize = 16.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    ),
                    cursorBrush = SolidColor(c.primary),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
            }
            Text(
                unitLabel,
                style = IntermTheme.typography.body.copy(fontSize = 13.sp),
                color = c.muted,
                modifier = Modifier.width(40.dp),
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(c.primary)
                    .clickable(onClick = onAdd)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Text(
                    "Add",
                    style = IntermTheme.typography.body.copy(fontSize = 13.sp, fontWeight = FontWeight.W500),
                    color = c.surface,
                )
            }
        }
    }
}

@Composable
private fun LogRow(
    entry: WaterEntryEntity,
    units: Units,
    isLast: Boolean,
    onDelete: () -> Unit,
) {
    val c = IntermTheme.colors
    val v = WaterMath.fmtVolume(entry.ml, units)
    val time = TimeMath.fmtTime(Instant.ofEpochMilli(entry.createdMs), ZoneId.systemDefault())
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(c.primarySoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(IntermIcons.Water, contentDescription = null, tint = c.primary)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${v.value} ${v.unit}",
                    style = IntermTheme.typography.body.copy(fontWeight = FontWeight.W500),
                    color = c.ink,
                )
                Spacer(Modifier.height(2.dp))
                Text(time, style = IntermTheme.typography.caption, color = c.muted)
            }
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onDelete),
                contentAlignment = Alignment.Center,
            ) {
                Icon(IntermIcons.Plus, contentDescription = "Delete", tint = c.muted, modifier = Modifier.rotate(45f))
            }
        }
        if (!isLast) Box(Modifier.fillMaxWidth().height(1.dp).background(c.border))
    }
}

@Composable
private fun SectionLabel(text: String) {
    val c = IntermTheme.colors
    Text(
        text.uppercase(),
        style = IntermTheme.typography.hEyebrow,
        color = c.muted,
        modifier = Modifier.padding(start = 4.dp),
    )
}

