package xyz.jishnu.health.ui.screens.onboarding

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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import xyz.jishnu.health.ui.components.DateOfBirthDialog
import xyz.jishnu.health.ui.components.HeightDialog
import xyz.jishnu.health.ui.components.IntermButton
import xyz.jishnu.health.ui.components.IntermButtonVariant
import xyz.jishnu.health.ui.components.IntermCard
import xyz.jishnu.health.ui.components.IntermIcons
import xyz.jishnu.health.ui.components.IntermTopBar
import xyz.jishnu.health.ui.components.SexSegmented
import xyz.jishnu.health.ui.components.StepDots3
import xyz.jishnu.health.ui.components.formatDateOfBirth
import xyz.jishnu.health.ui.components.formatHeightForDisplay
import xyz.jishnu.health.ui.theme.IntermTheme
import xyz.jishnu.health.vm.ProfileViewModel
import xyz.jishnu.health.vm.SettingsViewModel

@Composable
fun OnboardProfileScreen(
    onBack: () -> Unit,
    onContinue: () -> Unit,
    profileVm: ProfileViewModel = hiltViewModel(),
    settingsVm: SettingsViewModel = hiltViewModel(),
) {
    val profile by profileVm.profile.collectAsStateWithLifecycle()
    val settings by settingsVm.settings.collectAsStateWithLifecycle()
    val c = IntermTheme.colors

    var showHeight by remember { mutableStateOf(false) }
    var showDob by remember { mutableStateOf(false) }
    val canContinue = profile.sex != null && profile.heightCm != null && profile.dateOfBirth != null

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
                        Text("STEP 3 OF 5", style = IntermTheme.typography.hEyebrow, color = c.muted)
                        Spacer(Modifier.height(8.dp))
                        Text("Tell us a bit about you.", style = IntermTheme.typography.hTitle, color = c.ink)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Used to estimate ideal-weight ranges. Nothing leaves your device.",
                            style = IntermTheme.typography.body,
                            color = c.ink2,
                        )
                    }

                    SectionLabel("Sex")
                    SexSegmented(sex = profile.sex, onSelect = profileVm::setSex)

                    SectionLabel("Height")
                    InfoRow(
                        label = profile.heightCm?.let { formatHeightForDisplay(it, settings.units) } ?: "Set height",
                        sub = "Tap to edit",
                        onClick = { showHeight = true },
                    )

                    SectionLabel("Date of birth")
                    InfoRow(
                        label = profile.dateOfBirth?.let { formatDateOfBirth(it) } ?: "Set date of birth",
                        sub = "Tap to pick a date",
                        onClick = { showDob = true },
                    )
                }

                StepDots3(total = 5, currentStep = 3)
                Spacer(Modifier.height(18.dp))
                IntermButton(
                    onClick = onContinue,
                    variant = IntermButtonVariant.Primary,
                    fillWidth = true,
                    enabled = canContinue,
                ) {
                    Text("Continue")
                }
            }
        }
    }

    if (showHeight) {
        HeightDialog(
            currentCm = profile.heightCm,
            units = settings.units,
            sex = profile.sex,
            onDismiss = { showHeight = false },
            onConfirm = { cm -> profileVm.setHeightCm(cm); showHeight = false },
        )
    }
    if (showDob) {
        DateOfBirthDialog(
            currentIso = profile.dateOfBirth,
            onDismiss = { showDob = false },
            onConfirm = { iso -> profileVm.setDateOfBirth(iso); showDob = false },
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
private fun InfoRow(label: String, sub: String, onClick: () -> Unit) {
    val c = IntermTheme.colors
    IntermCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    label,
                    style = IntermTheme.typography.body.copy(fontWeight = FontWeight.W500, fontSize = 16.sp),
                    color = c.ink,
                )
                Spacer(Modifier.height(2.dp))
                Text(sub, style = IntermTheme.typography.caption, color = c.muted)
            }
            Icon(IntermIcons.Chevron, contentDescription = null, tint = c.subtle)
        }
    }
}
