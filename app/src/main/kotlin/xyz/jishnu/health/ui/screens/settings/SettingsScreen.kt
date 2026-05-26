package xyz.jishnu.health.ui.screens.settings

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import xyz.jishnu.health.data.constants.Plans
import xyz.jishnu.health.data.model.Units
import xyz.jishnu.health.domain.TimeMath
import xyz.jishnu.health.domain.WaterMath
import xyz.jishnu.health.ui.components.IntermIcons
import xyz.jishnu.health.ui.components.IntermSegmented
import xyz.jishnu.health.ui.components.IntermTopBar
import xyz.jishnu.health.ui.components.NavRow
import xyz.jishnu.health.ui.components.SegmentedOption
import xyz.jishnu.health.ui.components.TimeRow
import xyz.jishnu.health.ui.components.ToggleRow
import xyz.jishnu.health.ui.components.WaterGoalDialog
import xyz.jishnu.health.ui.components.rememberNotificationPermissionGranted
import xyz.jishnu.health.ui.theme.IntermTheme
import xyz.jishnu.health.vm.SettingsViewModel

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenPlanPicker: () -> Unit,
    vm: SettingsViewModel = hiltViewModel(),
) {
    val state by vm.settings.collectAsStateWithLifecycle()
    val c = IntermTheme.colors
    val plan = Plans.byId(state.planId)
    val fastEnd = TimeMath.addHoursToTime(state.fastStartTime, plan.fastHours.toDouble())

    val notifGranted by rememberNotificationPermissionGranted()
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* lifecycle resume re-checks granted state */ }
    val requestNotifPermission: () -> Unit = {
        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    Box(modifier = Modifier.fillMaxSize().background(c.bg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            IntermTopBar(
                modifier = Modifier.statusBarsPadding(),
                title = "Settings",
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
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
            ) {
                SectionLabel("Fasting")
                SettingsCard {
                    NavRow(
                        label = "Fasting protocol",
                        sub = "${plan.fastHours}h fast · ${plan.eatHours}h eating window",
                        trailing = plan.label,
                        onClick = onOpenPlanPicker,
                    )
                    ToggleRow(
                        label = "Fasting reminders",
                        sub = if (notifGranted) "Window start and end" else "Tap to grant notification permission",
                        checked = state.fastingReminderOn && notifGranted,
                        onCheckedChange = { newValue ->
                            if (!notifGranted) requestNotifPermission()
                            else vm.setFastingReminderOn(newValue)
                        },
                    )
                    TimeRow(
                        label = "Daily fasting start",
                        sub = "Ends at $fastEnd (${plan.label})",
                        value = state.fastStartTime,
                        onValueChange = vm::setFastStartTime,
                        enabled = state.fastingReminderOn && notifGranted,
                    )
                    ToggleRow(
                        label = "Sticky notification",
                        sub = if (notifGranted) "Show progress while fasting" else "Tap to grant notification permission",
                        checked = state.stickyNotificationOn && notifGranted,
                        onCheckedChange = { newValue ->
                            if (!notifGranted) requestNotifPermission()
                            else vm.setStickyNotificationOn(newValue)
                        },
                        showDivider = false,
                    )
                }

                SectionLabel("Weight")
                SettingsCard {
                    UnitsRow(units = state.units, onUnitsChange = vm::setUnits)
                    ToggleRow(
                        label = "Daily weigh-in reminder",
                        sub = if (notifGranted) null else "Tap to grant notification permission",
                        checked = state.weightReminderOn && notifGranted,
                        onCheckedChange = { newValue ->
                            if (!notifGranted) requestNotifPermission()
                            else vm.setWeightReminderOn(newValue)
                        },
                    )
                    TimeRow(
                        label = "Reminder time",
                        value = state.reminderTime,
                        onValueChange = vm::setReminderTime,
                        enabled = state.weightReminderOn && notifGranted,
                        showDivider = false,
                    )
                }

                SectionLabel("Water")
                SettingsCard {
                    val goalDisplay = WaterMath.fmtVolume(state.waterGoalMl, state.units)
                    var showGoalDialog by remember { mutableStateOf(false) }
                    NavRow(
                        label = "Daily water goal",
                        trailing = "${goalDisplay.value} ${goalDisplay.unit}",
                        onClick = { showGoalDialog = true },
                        showDivider = false,
                    )
                    if (showGoalDialog) {
                        WaterGoalDialog(
                            currentMl = state.waterGoalMl,
                            units = state.units,
                            onDismiss = { showGoalDialog = false },
                            onConfirm = { newMl ->
                                vm.setWaterGoalMl(newMl)
                                showGoalDialog = false
                            },
                        )
                    }
                }

                SectionLabel("About")
                SettingsCard {
                    NavRow(label = "Privacy policy", onClick = {})
                    NavRow(label = "Help & support", onClick = {})
                    NavRow(label = "Version", trailing = "0.1.0", showChevron = false, showDivider = false)
                }
                Spacer(Modifier.height(24.dp))
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
        modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 22.dp, bottom = 10.dp),
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
private fun UnitsRow(units: Units, onUnitsChange: (Units) -> Unit) {
    val c = IntermTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            "Units",
            modifier = Modifier.weight(1f),
            style = IntermTheme.typography.body.copy(fontWeight = FontWeight.W500),
            color = c.ink,
        )
        IntermSegmented(
            options = listOf(
                SegmentedOption(Units.Metric, "kg"),
                SegmentedOption(Units.Imperial, "lb"),
            ),
            selected = units,
            onSelect = onUnitsChange,
        )
    }
    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(c.border))
}
