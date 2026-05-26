package xyz.jishnu.health.ui.screens.onboarding

import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import xyz.jishnu.health.data.model.Units
import xyz.jishnu.health.domain.WeightMath
import xyz.jishnu.health.ui.components.IntermButton
import xyz.jishnu.health.ui.components.IntermButtonVariant
import xyz.jishnu.health.ui.components.IntermCard
import xyz.jishnu.health.ui.components.IntermIcons
import xyz.jishnu.health.ui.components.IntermTopBar
import xyz.jishnu.health.ui.components.StepDots3
import xyz.jishnu.health.ui.theme.IntermTheme
import xyz.jishnu.health.vm.SettingsViewModel
import xyz.jishnu.health.vm.WeightViewModel

@Composable
fun OnboardWeightScreen(
    onBack: () -> Unit,
    onContinue: () -> Unit,
    weightVm: WeightViewModel = hiltViewModel(),
    settingsVm: SettingsViewModel = hiltViewModel(),
) {
    val state by weightVm.state.collectAsStateWithLifecycle()
    val c = IntermTheme.colors
    val draftKg = state.draftKg ?: state.previous?.weightKg ?: 70.0
    val wf = WeightMath.fmtWeight(draftKg, state.units)
    val smallStepKg = WeightMath.deltaToKg(0.1, state.units)

    Box(modifier = Modifier.fillMaxSize().background(c.bg)) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            IntermTopBar(
                title = "",
                leading = {
                    Box(
                        modifier = Modifier.fillMaxSize().clickable(onClick = onBack),
                        contentAlignment = Alignment.Center,
                    ) { Icon(IntermIcons.Back, contentDescription = "Back", tint = c.ink2) }
                },
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp)
                    .padding(top = 8.dp, bottom = 28.dp),
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    Column {
                        Text("STEP 3 OF 4", style = IntermTheme.typography.hEyebrow, color = c.muted)
                        Spacer(Modifier.height(8.dp))
                        Text("What do you weigh today?", style = IntermTheme.typography.hTitle, color = c.ink)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "We'll use this as your starting point and to size your daily water goal.",
                            style = IntermTheme.typography.body,
                            color = c.ink2,
                        )
                    }

                    SectionLabel("Units")
                    UnitsCard(units = state.units, onSelect = settingsVm::setUnits)

                    IntermCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                StepperButton(IntermIcons.Minus) { weightVm.bumpDraftKg(-smallStepKg) }
                                Spacer(Modifier.width(14.dp))
                                Row(
                                    modifier = Modifier.defaultMinSize(minWidth = 160.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.Bottom,
                                ) {
                                    Text(
                                        wf.value,
                                        style = IntermTheme.typography.hDisplay.copy(fontSize = 56.sp),
                                        color = c.ink,
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        wf.unit,
                                        style = IntermTheme.typography.body.copy(fontSize = 18.sp),
                                        color = c.muted,
                                        modifier = Modifier.padding(bottom = 8.dp),
                                    )
                                }
                                Spacer(Modifier.width(14.dp))
                                StepperButton(IntermIcons.Plus) { weightVm.bumpDraftKg(smallStepKg) }
                            }
                            Spacer(Modifier.height(16.dp))
                            QuickDeltaRow(units = state.units, onDelta = { d ->
                                weightVm.bumpDraftKg(WeightMath.deltaToKg(d, state.units))
                            })
                        }
                    }
                }

                StepDots3(total = 4, currentStep = 3)
                Spacer(Modifier.height(18.dp))
                IntermButton(
                    onClick = { weightVm.save { onContinue() } },
                    variant = IntermButtonVariant.Primary,
                    fillWidth = true,
                ) {
                    Text("Continue")
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        style = IntermTheme.typography.hEyebrow,
        color = IntermTheme.colors.muted,
    )
}

@Composable
private fun UnitsCard(units: Units, onSelect: (Units) -> Unit) {
    val c = IntermTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(c.card)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        UnitsTab(
            label = "Metric (kg)",
            active = units == Units.Metric,
            onClick = { onSelect(Units.Metric) },
            modifier = Modifier.weight(1f),
        )
        UnitsTab(
            label = "Imperial (lb)",
            active = units == Units.Imperial,
            onClick = { onSelect(Units.Imperial) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun UnitsTab(label: String, active: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = IntermTheme.colors
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (active) c.primarySoft else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = IntermTheme.typography.body.copy(fontSize = 14.sp, fontWeight = if (active) FontWeight.W600 else FontWeight.W500),
            color = if (active) c.primary else c.ink2,
        )
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
