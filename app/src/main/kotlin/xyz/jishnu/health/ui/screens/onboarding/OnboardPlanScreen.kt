package xyz.jishnu.health.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import xyz.jishnu.health.data.constants.Plans
import xyz.jishnu.health.ui.components.IntermButton
import xyz.jishnu.health.ui.components.IntermButtonVariant
import xyz.jishnu.health.ui.components.IntermIcons
import xyz.jishnu.health.ui.components.IntermTopBar
import xyz.jishnu.health.ui.components.PlanRadio
import xyz.jishnu.health.ui.components.StepDots3
import xyz.jishnu.health.ui.theme.IntermTheme
import xyz.jishnu.health.vm.SettingsViewModel

@Composable
fun OnboardPlanScreen(
    onBack: () -> Unit,
    onContinue: () -> Unit,
    vm: SettingsViewModel = hiltViewModel(),
) {
    val state by vm.settings.collectAsStateWithLifecycle()
    val c = IntermTheme.colors

    var selected by remember { mutableStateOf(state.planId) }
    LaunchedEffect(state.planId) { selected = state.planId }

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
                ) {
                    Text("STEP 2 OF 3", style = IntermTheme.typography.hEyebrow, color = c.muted)
                    Spacer(Modifier.height(8.dp))
                    Text("Choose a fasting plan.", style = IntermTheme.typography.hTitle, color = c.ink)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "You can change this any time in settings.",
                        style = IntermTheme.typography.body,
                        color = c.ink2,
                    )
                    Spacer(Modifier.height(20.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Plans.all.forEach { p ->
                            PlanRadio(plan = p, selected = selected == p.id, onSelect = { selected = p.id })
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }
                StepDots3(currentStep = 2)
                Spacer(Modifier.height(18.dp))
                IntermButton(
                    onClick = { vm.setPlanId(selected); onContinue() },
                    variant = IntermButtonVariant.Primary,
                    fillWidth = true,
                ) {
                    Text("Continue")
                }
            }
        }
    }
}
