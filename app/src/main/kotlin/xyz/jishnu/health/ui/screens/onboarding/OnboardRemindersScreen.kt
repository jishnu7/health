package xyz.jishnu.health.ui.screens.onboarding

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import xyz.jishnu.health.data.constants.Plans
import xyz.jishnu.health.data.model.Units
import xyz.jishnu.health.domain.TimeMath
import xyz.jishnu.health.ui.components.IntermButton
import xyz.jishnu.health.ui.components.IntermButtonVariant
import xyz.jishnu.health.ui.components.IntermCard
import xyz.jishnu.health.ui.components.IntermIcons
import xyz.jishnu.health.ui.components.IntermToggle
import xyz.jishnu.health.ui.components.IntermTopBar
import xyz.jishnu.health.ui.components.StepDots3
import xyz.jishnu.health.ui.components.TimePickerDialog
import xyz.jishnu.health.ui.theme.IntermTheme
import xyz.jishnu.health.vm.FastingViewModel
import xyz.jishnu.health.vm.SettingsViewModel

@Composable
fun OnboardRemindersScreen(
    onBack: () -> Unit,
    onFinish: () -> Unit,
    settingsVm: SettingsViewModel = hiltViewModel(),
    fastingVm: FastingViewModel,
) {
    val state by settingsVm.settings.collectAsStateWithLifecycle()
    val c = IntermTheme.colors
    val plan = Plans.byId(state.planId)
    val fastEnd = TimeMath.addHoursToTime(state.fastStartTime, plan.fastHours.toDouble())

    var showFastStart by remember { mutableStateOf(false) }
    var showReminder by remember { mutableStateOf(false) }

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
                        Text("STEP 3 OF 3", style = IntermTheme.typography.hEyebrow, color = c.muted)
                        Spacer(Modifier.height(8.dp))
                        Text("A few preferences.", style = IntermTheme.typography.hTitle, color = c.ink)
                    }

                    SectionLabel("Units")
                    UnitsCard(units = state.units, onSelect = settingsVm::setUnits)

                    SectionLabel("Fasting start")
                    PreferenceCard(
                        icon = IntermIcons.Flame,
                        label = "Start each day at",
                        sub = "Ends at $fastEnd · ${plan.label}",
                        valueText = state.fastStartTime,
                        onClick = { showFastStart = true },
                    )

                    SectionLabel("Daily weigh-in")
                    PreferenceCard(
                        icon = IntermIcons.Bell,
                        label = "Remind me at",
                        sub = "Every day, gentle nudge",
                        valueText = state.reminderTime,
                        onClick = { showReminder = true },
                    )

                    IntermCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Fasting reminders",
                                    style = IntermTheme.typography.body.copy(fontWeight = FontWeight.W500),
                                    color = c.ink,
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    "When a fast is about to start or end",
                                    style = IntermTheme.typography.caption,
                                    color = c.muted,
                                )
                            }
                            IntermToggle(
                                checked = state.fastingReminderOn,
                                onCheckedChange = settingsVm::setFastingReminderOn,
                            )
                        }
                    }
                }

                StepDots3(currentStep = 3)
                Spacer(Modifier.height(18.dp))
                IntermButton(
                    onClick = {
                        fastingVm.startFast()
                        settingsVm.markOnboarded()
                        onFinish()
                    },
                    variant = IntermButtonVariant.Primary,
                    fillWidth = true,
                ) {
                    Text("Start Fasting")
                }
            }
        }
    }

    if (showFastStart) {
        TimePickerDialog(
            initial = state.fastStartTime,
            onDismiss = { showFastStart = false },
            onConfirm = { showFastStart = false; settingsVm.setFastStartTime(it) },
        )
    }
    if (showReminder) {
        TimePickerDialog(
            initial = state.reminderTime,
            onDismiss = { showReminder = false },
            onConfirm = { showReminder = false; settingsVm.setReminderTime(it) },
        )
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
private fun PreferenceCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    sub: String,
    valueText: String,
    onClick: () -> Unit,
) {
    val c = IntermTheme.colors
    IntermCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        contentPadding = PaddingValues(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Icon(icon, contentDescription = null, tint = c.ink2)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    label,
                    style = IntermTheme.typography.body.copy(fontWeight = FontWeight.W500),
                    color = c.ink,
                )
                Spacer(Modifier.height(2.dp))
                Text(sub, style = IntermTheme.typography.caption, color = c.muted)
            }
            Text(
                valueText,
                style = IntermTheme.typography.mono.copy(fontSize = 18.sp, fontWeight = FontWeight.W500),
                color = c.ink,
            )
        }
    }
}
