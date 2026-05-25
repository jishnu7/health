package xyz.jishnu.health.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import xyz.jishnu.health.data.constants.Plans
import xyz.jishnu.health.data.model.Plan
import xyz.jishnu.health.ui.components.IntermButton
import xyz.jishnu.health.ui.components.IntermButtonVariant
import xyz.jishnu.health.ui.components.IntermIcons
import xyz.jishnu.health.ui.components.IntermTopBar
import xyz.jishnu.health.ui.theme.IntermTheme
import xyz.jishnu.health.vm.SettingsViewModel

@Composable
fun PlanPickerScreen(
    onBack: () -> Unit,
    vm: SettingsViewModel = hiltViewModel(),
) {
    val state by vm.settings.collectAsStateWithLifecycle()
    val c = IntermTheme.colors

    var selected by remember { mutableStateOf(state.planId) }
    LaunchedEffect(state.planId) { selected = state.planId }

    Box(modifier = Modifier.fillMaxSize().background(c.bg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            IntermTopBar(
                modifier = Modifier.statusBarsPadding(),
                title = "Fasting protocol",
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
                Spacer(Modifier.height(4.dp))
                Text(
                    "Choose how long you want to fast each day. You can change this any time.",
                    style = IntermTheme.typography.body,
                    color = c.ink2,
                )
                Spacer(Modifier.height(18.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Plans.all.forEach { p -> PlanRadio(plan = p, selected = selected == p.id, onSelect = { selected = p.id }) }
                }
                Spacer(Modifier.height(22.dp))
                IntermButton(
                    onClick = { vm.setPlanId(selected); onBack() },
                    variant = IntermButtonVariant.Primary,
                    fillWidth = true,
                ) {
                    Icon(IntermIcons.Check, contentDescription = null)
                    Text("Save")
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun PlanRadio(plan: Plan, selected: Boolean, onSelect: () -> Unit) {
    val c = IntermTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) c.primarySoft else c.card)
            .border(width = 1.5.dp, color = if (selected) c.primary else c.border, shape = RoundedCornerShape(14.dp))
            .clickable(onClick = onSelect)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(c.bg)
                .border(1.dp, c.border, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                plan.label,
                style = IntermTheme.typography.mono.copy(fontSize = 16.sp, fontWeight = FontWeight.W600),
                color = c.ink,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "${plan.fastHours}h fasting",
                style = IntermTheme.typography.body.copy(fontSize = 15.sp, fontWeight = FontWeight.W500),
                color = c.ink,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "${plan.subtitle} · ${plan.eatHours}h eating window",
                style = IntermTheme.typography.caption,
                color = c.muted,
            )
        }
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (selected) c.primary else Color.Transparent)
                .border(1.5.dp, if (selected) c.primary else c.border, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Icon(IntermIcons.Check, contentDescription = null, tint = c.surface, modifier = Modifier.size(14.dp))
        }
    }
}
