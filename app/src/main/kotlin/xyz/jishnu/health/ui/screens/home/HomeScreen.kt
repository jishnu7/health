package xyz.jishnu.health.ui.screens.home

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.jishnu.health.ui.components.BottomNav
import xyz.jishnu.health.ui.components.IntermButton
import xyz.jishnu.health.ui.components.IntermButtonVariant
import xyz.jishnu.health.ui.components.IntermCard
import xyz.jishnu.health.ui.components.IntermIcons
import xyz.jishnu.health.ui.components.IntermStageChip
import xyz.jishnu.health.ui.components.IntermTopBar
import xyz.jishnu.health.ui.components.NavTab
import xyz.jishnu.health.ui.components.ProgressRing
import xyz.jishnu.health.ui.components.StageDots
import xyz.jishnu.health.ui.theme.IntermTheme
import xyz.jishnu.health.vm.FastingUiState
import xyz.jishnu.health.vm.FastingViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import xyz.jishnu.health.domain.TimeMath
import java.time.Instant

@Composable
fun HomeScreen(
    vm: FastingViewModel,
    onNavigateTab: (NavTab) -> Unit,
    onOpenStages: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val c = IntermTheme.colors

    Box(modifier = Modifier.fillMaxSize().background(c.bg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            IntermTopBar(
                modifier = Modifier.statusBarsPadding(),
                title = "Interm",
                trailing = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(onClick = onOpenSettings),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(IntermIcons.Settings, contentDescription = "Settings", tint = c.ink2)
                    }
                },
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
            ) {
                if (state.isFasting) ActiveBody(state, onOpenStages, { vm.endFast() }, { vm.resetFast() })
                else IdleBody(state, onStart = { vm.startFast() }, onLogWeight = { onNavigateTab(NavTab.Weight) }, onOpenSettings = onOpenSettings)
                Spacer(Modifier.height(20.dp))
            }
            BottomNav(
                active = NavTab.Today,
                onChange = onNavigateTab,
                modifier = Modifier.navigationBarsPadding(),
            )
        }
    }
}

@Composable
private fun ActiveBody(
    state: FastingUiState,
    onOpenStages: () -> Unit,
    onEnd: () -> Unit,
    onReset: () -> Unit,
) {
    val c = IntermTheme.colors
    val d = TimeMath.fmtDuration(state.elapsedMs)
    val dr = TimeMath.fmtDuration(state.remainingMs)
    val startedAt = state.fastStartMs?.let { Instant.ofEpochMilli(it) }
    val goalAt = state.fastEndMs?.let { Instant.ofEpochMilli(it) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        IntermStageChip(label = "Stage ${state.stageIdx + 1} of ${state.stages.size} · ${state.stage.name}")

        ProgressRing(progress = state.progress, size = 250.dp, stroke = 12.dp) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("ELAPSED", style = IntermTheme.typography.hEyebrow, color = c.muted)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(d.hh, style = IntermTheme.typography.hDisplay, color = c.ink)
                    Text(":", style = IntermTheme.typography.hDisplay, color = c.muted)
                    Text(d.mm, style = IntermTheme.typography.hDisplay, color = c.ink)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "${d.ss}s · ${(state.progress * 100).toInt()}% of ${state.goalHours}h",
                    style = IntermTheme.typography.caption,
                    color = c.muted,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TimeMarker("Started", startedAt?.let { TimeMath.fmtTime(it) } ?: "—", Alignment.Start)
            TimeMarker("Remaining", "${dr.hours}h ${dr.mm}m", Alignment.CenterHorizontally)
            TimeMarker("Goal", goalAt?.let { TimeMath.fmtTime(it) } ?: "—", Alignment.End)
        }

        StageDots(count = state.stages.size, currentIdx = state.stageIdx)

        IntermCard(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenStages),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("CURRENT STAGE", style = IntermTheme.typography.hEyebrow, color = c.muted)
                Icon(IntermIcons.Chevron, contentDescription = null, tint = c.muted)
            }
            Spacer(Modifier.height(8.dp))
            Text(state.stage.title, style = IntermTheme.typography.headerTitle.copy(fontSize = 18.sp), color = c.ink)
            Spacer(Modifier.height(6.dp))
            Text(state.stage.body, style = IntermTheme.typography.body, color = c.ink2)
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            IntermButton(onClick = onEnd, variant = IntermButtonVariant.Soft, modifier = Modifier.weight(1f), fillWidth = false) {
                Icon(IntermIcons.Stop, contentDescription = null)
                Text("End fast")
            }
            IntermButton(onClick = onReset, variant = IntermButtonVariant.Danger, modifier = Modifier.weight(1f), fillWidth = false) {
                Icon(IntermIcons.Food, contentDescription = null)
                Text("I ate")
            }
        }
    }
}

@Composable
private fun IdleBody(
    state: FastingUiState,
    onStart: () -> Unit,
    onLogWeight: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val c = IntermTheme.colors
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("NOT FASTING", style = IntermTheme.typography.hEyebrow, color = c.muted)
            Spacer(Modifier.height(8.dp))
            Text("Ready when you are.", style = IntermTheme.typography.hTitle, color = c.ink)
            Spacer(Modifier.height(10.dp))
            Text(
                "Start a ${state.plan.label} fast. We'll track your progress through each metabolic phase.",
                style = IntermTheme.typography.body,
                color = c.ink2,
                modifier = Modifier.widthIn(max = 280.dp),
            )
        }

        ProgressRing(progress = 0f, size = 240.dp, stroke = 12.dp, dashed = true) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("GOAL", style = IntermTheme.typography.hEyebrow, color = c.muted)
                Spacer(Modifier.height(6.dp))
                Text("${state.goalHours}h", style = IntermTheme.typography.hDisplay.copy(fontSize = 48.sp), color = c.ink)
                Spacer(Modifier.height(6.dp))
                Text("${state.plan.label} · ${state.plan.subtitle}", style = IntermTheme.typography.caption, color = c.muted)
            }
        }

        IntermButton(onClick = onStart, variant = IntermButtonVariant.Primary, fillWidth = true) {
            Icon(IntermIcons.Play, contentDescription = null)
            Text("Start fasting")
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IntermButton(
                onClick = onLogWeight,
                variant = IntermButtonVariant.Ghost,
                modifier = Modifier.weight(1f).height(44.dp),
            ) {
                Icon(IntermIcons.Scale, contentDescription = null)
                Text("Log weight")
            }
            IntermButton(
                onClick = onOpenSettings,
                variant = IntermButtonVariant.Ghost,
                modifier = Modifier.weight(1f).height(44.dp),
            ) {
                Text("Change plan")
            }
        }
    }
}

@Composable
private fun TimeMarker(label: String, value: String, align: Alignment.Horizontal) {
    val c = IntermTheme.colors
    Column(horizontalAlignment = align) {
        Text(label, style = IntermTheme.typography.caption, color = c.muted)
        Spacer(Modifier.height(2.dp))
        Text(value, style = IntermTheme.typography.body.copy(fontWeight = FontWeight.W500), color = c.ink)
    }
}
