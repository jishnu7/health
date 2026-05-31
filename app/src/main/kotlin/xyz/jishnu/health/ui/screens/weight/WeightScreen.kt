package xyz.jishnu.health.ui.screens.weight

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import xyz.jishnu.health.data.model.Sex
import xyz.jishnu.health.data.model.Units
import xyz.jishnu.health.domain.IdealFormula
import xyz.jishnu.health.domain.IdealWeight
import xyz.jishnu.health.domain.WeightMath
import xyz.jishnu.health.ui.components.BottomNav
import xyz.jishnu.health.ui.components.IntermButton
import xyz.jishnu.health.ui.components.IntermButtonVariant
import xyz.jishnu.health.ui.components.IntermCard
import xyz.jishnu.health.ui.components.IntermIcons
import xyz.jishnu.health.ui.components.IntermTopBar
import xyz.jishnu.health.ui.components.NavTab
import xyz.jishnu.health.ui.theme.IntermTheme
import xyz.jishnu.health.vm.WeightViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

@Composable
fun WeightScreen(
    onNavigateTab: (NavTab) -> Unit,
    onBack: () -> Unit,
    vm: WeightViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val c = IntermTheme.colors
    val draftKg = state.draftKg ?: state.previous?.weightKg ?: 70.0
    val wf = WeightMath.fmtWeight(draftKg, state.units)
    val prevKg = state.previous?.weightKg
    val trendKg = prevKg?.let { draftKg - it } ?: 0.0
    val trendW = WeightMath.fmtWeight(abs(trendKg), state.units)
    val avgKg = state.sevenDayAverageKg
    val avgW = avgKg?.let { WeightMath.fmtWeight(it, state.units) }
    // Stepper / quick-delta values are expressed in the user's displayed unit
    // (kg in metric, lb in imperial) and converted to kg before persisting.
    val smallStepKg = WeightMath.deltaToKg(0.1, state.units)

    var showIdealWeight by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(c.bg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            IntermTopBar(
                modifier = Modifier.statusBarsPadding(),
                title = "Weight",
                leading = {
                    Box(
                        modifier = Modifier.fillMaxSize().clickable(onClick = onBack),
                        contentAlignment = Alignment.Center,
                    ) { Icon(IntermIcons.Back, contentDescription = "Back", tint = c.ink2) }
                },
                trailing = {
                    Box(
                        modifier = Modifier.fillMaxSize().clickable { showIdealWeight = true },
                        contentAlignment = Alignment.Center,
                    ) { Icon(IntermIcons.Help, contentDescription = "Ideal weight", tint = c.ink2) }
                },
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            ) {
                IntermCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            todayLabel(),
                            style = IntermTheme.typography.hEyebrow,
                            color = c.muted,
                        )
                        Spacer(Modifier.height(18.dp))
                        StepperRow(
                            value = wf.value,
                            unit = wf.unit,
                            onMinus = { vm.bumpDraftKg(-smallStepKg) },
                            onPlus = { vm.bumpDraftKg(smallStepKg) },
                        )
                        Spacer(Modifier.height(18.dp))
                        QuickDeltaRow(units = state.units, onDelta = { displayDelta ->
                            vm.bumpDraftKg(WeightMath.deltaToKg(displayDelta, state.units))
                        })
                        Spacer(Modifier.height(22.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(c.border))
                        Spacer(Modifier.height(16.dp))
                        SummaryRow(
                            prev = prevKg?.let { WeightMath.fmtWeight(it, state.units) },
                            trend = trendKg,
                            trendValue = trendW,
                            avg = avgW,
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))
                Text("NOTES (OPTIONAL)", style = IntermTheme.typography.hEyebrow, color = c.muted)
                Spacer(Modifier.height(10.dp))
                IntermCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Tap to add a note about today…", style = IntermTheme.typography.body, color = c.muted)
                }

                Spacer(Modifier.height(22.dp))
                IntermButton(
                    onClick = { vm.save { onNavigateTab(NavTab.Today) } },
                    variant = IntermButtonVariant.Primary,
                    fillWidth = true,
                ) {
                    Icon(IntermIcons.Check, contentDescription = null)
                    Text("Save Entry")
                }
                Spacer(Modifier.height(20.dp))
            }
            BottomNav(
                active = NavTab.Weight,
                onChange = onNavigateTab,
                modifier = Modifier.navigationBarsPadding(),
            )
        }
    }

    if (showIdealWeight) {
        IdealWeightDialog(
            heightCm = state.heightCm,
            sex = state.sex,
            units = state.units,
            onDismiss = { showIdealWeight = false },
        )
    }
}

@Composable
private fun IdealWeightDialog(
    heightCm: Double?,
    sex: Sex?,
    units: Units,
    onDismiss: () -> Unit,
) {
    val c = IntermTheme.colors
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(22.dp))
                .background(c.card)
                .padding(20.dp),
        ) {
            Text("Ideal weight", style = IntermTheme.typography.headerTitle, color = c.ink)
            Spacer(Modifier.height(6.dp))

            if (heightCm == null || sex == null) {
                Text(
                    "Add your height and sex in Settings → Profile to see your ideal-weight ranges.",
                    style = IntermTheme.typography.caption,
                    color = c.muted,
                )
            } else {
                val heightLabel = formatHeight(heightCm, units)
                val sexLabel = when (sex) { Sex.Male -> "male"; Sex.Female -> "female" }
                Text(
                    "Based on $heightLabel · $sexLabel.",
                    style = IntermTheme.typography.caption,
                    color = c.muted,
                )
                Spacer(Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                        .background(c.border2)
                        .padding(horizontal = 14.dp),
                ) {
                    val formulas = IdealFormula.entries
                    formulas.forEachIndexed { idx, formula ->
                        val kg = formula.calculate(heightCm, sex).coerceAtLeast(0.0)
                        val fmt = WeightMath.fmtWeight(kg, units)
                        IdealRow(
                            label = formula.label,
                            value = "${fmt.value} ${fmt.unit}",
                            sub = "${formula.year} formula",
                        )
                        Box(Modifier.fillMaxWidth().height(1.dp).background(c.border))
                        if (idx == formulas.lastIndex) {
                            val (lowKg, highKg) = with(IdealWeight.healthyBmiRange(heightCm)) { start to endInclusive }
                            val lowFmt = WeightMath.fmtWeight(lowKg, units)
                            val highFmt = WeightMath.fmtWeight(highKg, units)
                            IdealRow(
                                label = "Healthy BMI",
                                value = "${lowFmt.value} – ${highFmt.value} ${highFmt.unit}",
                                sub = "WHO 18.5 – 24.9",
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IntermButton(onClick = onDismiss, variant = IntermButtonVariant.Primary) {
                    Text("Got it")
                }
            }
        }
    }
}

@Composable
private fun IdealRow(label: String, value: String, sub: String) {
    val c = IntermTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = IntermTheme.typography.body.copy(fontWeight = FontWeight.W500), color = c.ink)
            Spacer(Modifier.height(2.dp))
            Text(sub, style = IntermTheme.typography.caption, color = c.muted)
        }
        Text(
            value,
            style = IntermTheme.typography.mono.copy(fontSize = 15.sp, fontWeight = FontWeight.W500),
            color = c.ink,
        )
    }
}

private fun formatHeight(cm: Double, units: Units): String = when (units) {
    Units.Metric -> "${cm.toInt()} cm"
    Units.Imperial -> {
        val totalInches = cm / 2.54
        val feet = (totalInches / 12).toInt()
        val inches = (totalInches - feet * 12).toInt()
        "$feet'${inches}\""
    }
}

@Composable
private fun StepperRow(value: String, unit: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    val c = IntermTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        StepperButton(IntermIcons.Minus, onClick = onMinus)
        Spacer(Modifier.width(14.dp))
        Row(
            modifier = Modifier.defaultMinSize(minWidth = 180.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                value,
                style = IntermTheme.typography.hDisplay.copy(fontSize = 64.sp),
                color = c.ink,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                unit,
                style = IntermTheme.typography.body.copy(fontSize = 20.sp, fontWeight = FontWeight.W400),
                color = c.muted,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        StepperButton(IntermIcons.Plus, onClick = onPlus)
    }
}

@Composable
private fun StepperButton(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    val c = IntermTheme.colors
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(c.border2)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = c.ink2)
    }
}

/**
 * Quick-delta pill row. Deltas are emitted in the **displayed unit** — callers convert
 * to kg before writing. So +1 in metric mode means +1 kg; +1 in imperial means +1 lb.
 */
@Composable
private fun QuickDeltaRow(units: Units, onDelta: (Double) -> Unit) {
    val c = IntermTheme.colors
    val deltas = listOf(-1.0, -0.1, 0.1, 1.0)
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(c.border2)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        deltas.forEach { d ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .clickable { onDelta(d) }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                val label = (if (d > 0) "+" else "") + d.toString()
                Text(label, style = IntermTheme.typography.body.copy(fontSize = 13.sp), color = c.muted)
            }
        }
    }
}

@Composable
private fun SummaryRow(
    prev: WeightMath.FormattedWeight?,
    trend: Double,
    trendValue: WeightMath.FormattedWeight,
    avg: WeightMath.FormattedWeight?,
) {
    val c = IntermTheme.colors
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        SummaryItem("Previous", prev?.let { "${it.value} ${it.unit}" } ?: "—", c.ink)
        val trendColor = if (trend < 0) c.primary else if (trend > 0) c.accent else c.ink
        val trendText = "${if (trend < 0) "−" else if (trend > 0) "+" else ""}${trendValue.value} ${trendValue.unit}"
        SummaryItem("Change", if (prev == null) "—" else trendText, trendColor)
        SummaryItem("7-day avg", avg?.let { "${it.value} ${it.unit}" } ?: "—", c.ink)
    }
}

@Composable
private fun SummaryItem(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = IntermTheme.typography.caption, color = IntermTheme.colors.muted)
        Spacer(Modifier.height(2.dp))
        Text(
            value,
            style = IntermTheme.typography.mono.copy(fontSize = 15.sp, fontWeight = FontWeight.W500),
            color = valueColor,
        )
    }
}

private fun todayLabel(): String =
    LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault()))
