package xyz.jishnu.health.ui.screens.onboarding

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import xyz.jishnu.health.data.constants.Plans
import xyz.jishnu.health.domain.TimeMath
import xyz.jishnu.health.ui.components.IntermButton
import xyz.jishnu.health.ui.components.IntermButtonVariant
import xyz.jishnu.health.ui.components.IntermCard
import xyz.jishnu.health.ui.components.IntermIcons
import xyz.jishnu.health.ui.components.IntermToggle
import xyz.jishnu.health.ui.components.IntermTopBar
import xyz.jishnu.health.ui.components.StepDots3
import xyz.jishnu.health.ui.components.TimePickerDialog
import xyz.jishnu.health.ui.components.rememberNotificationPermissionGranted
import xyz.jishnu.health.ui.theme.IntermTheme
import xyz.jishnu.health.vm.SettingsViewModel

@Composable
fun OnboardNotificationsScreen(
    onBack: () -> Unit,
    onFinish: () -> Unit,
    settingsVm: SettingsViewModel = hiltViewModel(),
) {
    val state by settingsVm.settings.collectAsStateWithLifecycle()
    val c = IntermTheme.colors
    val plan = Plans.byId(state.planId)
    val fastEnd = TimeMath.addHoursToTime(state.fastStartTime, plan.fastHours.toDouble())

    var showFastStart by remember { mutableStateOf(false) }
    var showReminder by remember { mutableStateOf(false) }

    val notifGranted by rememberNotificationPermissionGranted()
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* lifecycle resume re-checks granted state */ }
    val requestNotifPermission: () -> Unit = {
        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    LaunchedEffect(Unit) {
        if (!notifGranted) requestNotifPermission()
    }

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
                        Text("STEP 5 OF 5", style = IntermTheme.typography.hEyebrow, color = c.muted)
                        Spacer(Modifier.height(8.dp))
                        Text("Stay on track with reminders.", style = IntermTheme.typography.hTitle, color = c.ink)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Allow notifications to get gentle nudges for fast windows and your daily weigh-in.",
                            style = IntermTheme.typography.body,
                            color = c.ink2,
                        )
                    }

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

                    ReminderToggleCard(
                        label = "Fasting reminders",
                        subOn = "When a fast is about to start or end",
                        subOff = "Tap to grant notification permission",
                        checked = state.fastingReminderOn && notifGranted,
                        notifGranted = notifGranted,
                        onCheckedChange = { newValue ->
                            if (!notifGranted) requestNotifPermission()
                            else settingsVm.setFastingReminderOn(newValue)
                        },
                    )
                    ReminderToggleCard(
                        label = "Hydration reminders",
                        subOn = "Paced nudges when you fall behind",
                        subOff = "Tap to grant notification permission",
                        checked = state.waterReminderOn && notifGranted,
                        notifGranted = notifGranted,
                        onCheckedChange = { newValue ->
                            if (!notifGranted) requestNotifPermission()
                            else settingsVm.setWaterReminderOn(newValue)
                        },
                    )
                }

                StepDots3(total = 5, currentStep = 5)
                Spacer(Modifier.height(18.dp))
                IntermButton(
                    onClick = {
                        settingsVm.markOnboarded()
                        onFinish()
                    },
                    variant = IntermButtonVariant.Primary,
                    fillWidth = true,
                ) {
                    Text("Finish")
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
private fun ReminderToggleCard(
    label: String,
    subOn: String,
    subOff: String,
    checked: Boolean,
    notifGranted: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val c = IntermTheme.colors
    IntermCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    label,
                    style = IntermTheme.typography.body.copy(fontWeight = FontWeight.W500),
                    color = c.ink,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    if (notifGranted) subOn else subOff,
                    style = IntermTheme.typography.caption,
                    color = c.muted,
                )
            }
            IntermToggle(checked = checked, onCheckedChange = onCheckedChange)
        }
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
