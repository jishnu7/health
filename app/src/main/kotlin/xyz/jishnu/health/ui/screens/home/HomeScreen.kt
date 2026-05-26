package xyz.jishnu.health.ui.screens.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import xyz.jishnu.health.domain.TimeMath
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
import java.time.Instant

private val RingSize = 250.dp
private val RingStroke = 12.dp

@Composable
fun HomeScreen(
    vm: FastingViewModel,
    onNavigateTab: (NavTab) -> Unit,
    onOpenStages: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val c = IntermTheme.colors

    val ringProgress = remember { Animatable(0f) }
    var sweeping by remember { mutableStateOf(false) }
    // A locally-driven mirror of `state.isFasting`. We hold it back until the
    // sweep peaks so the center / chip / body crossfades line up with the ring
    // refilling from zero, instead of fading on top of a still-sweeping ring.
    var displayFasting by remember { mutableStateOf(state.isFasting) }
    val scope = rememberCoroutineScope()

    // Whenever the underlying state changes outside of a sweep (process restart,
    // resetFast, …) catch the display state up so the two never get stuck out
    // of sync.
    LaunchedEffect(state.isFasting, sweeping) {
        if (!sweeping) displayFasting = state.isFasting
    }

    LaunchedEffect(state.fastStartMs) {
        if (state.fastStartMs != null) {
            sweeping = true
            displayFasting = false
            ringProgress.snapTo(0f)
            ringProgress.animateTo(1f, tween(durationMillis = 900, easing = FastOutSlowInEasing))
            // Sweep just peaked. Reset the ring and reveal the active state by
            // letting it progress up to its real value with a calm ease so the
            // numbers and chip fade in over the same motion.
            displayFasting = true
            ringProgress.snapTo(0f)
            sweeping = false
            ringProgress.animateTo(
                targetValue = state.progress,
                animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
            )
        }
    }

    LaunchedEffect(state.progress, sweeping, state.isFasting) {
        if (!sweeping && state.isFasting) {
            ringProgress.animateTo(
                targetValue = state.progress,
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
            )
        } else if (!state.isFasting && !sweeping) {
            ringProgress.snapTo(0f)
        }
    }

    val onEndWithSweep: () -> Unit = {
        scope.launch {
            sweeping = true
            val from = ringProgress.value
            ringProgress.snapTo(from)
            ringProgress.animateTo(1f, tween(durationMillis = 900, easing = FastOutSlowInEasing))
            // Sweep peaks → reveal idle state with the same calm down-to-zero
            // motion as the start animation's up-to-progress motion.
            displayFasting = false
            ringProgress.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
            )
            sweeping = false
            vm.endFast()
        }
    }

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
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(16.dp))

                ChipSlot(state = state, displayFasting = displayFasting)

                Spacer(Modifier.height(22.dp))

                ProgressRing(
                    progress = ringProgress.value,
                    size = RingSize,
                    stroke = RingStroke,
                    dashed = sweeping || !displayFasting,
                ) {
                    AnimatedContent(
                        targetState = displayFasting,
                        transitionSpec = {
                            fadeIn(tween(durationMillis = 500, easing = FastOutSlowInEasing)) togetherWith
                                fadeOut(tween(durationMillis = 220, easing = FastOutSlowInEasing))
                        },
                        label = "ring-center",
                    ) { fasting ->
                        if (fasting) ActiveRingCenter(state) else IdleRingCenter(state)
                    }
                }

                Spacer(Modifier.height(22.dp))

                AnimatedContent(
                    targetState = displayFasting,
                    transitionSpec = {
                        fadeIn(tween(durationMillis = 520, easing = FastOutSlowInEasing)) togetherWith
                            fadeOut(tween(durationMillis = 220, easing = FastOutSlowInEasing))
                    },
                    label = "home-body",
                ) { fasting ->
                    if (fasting) {
                        ActiveBody(
                            state = state,
                            onOpenStages = onOpenStages,
                            onEnd = onEndWithSweep,
                            onReset = { vm.resetFast() },
                        )
                    } else {
                        IdleBody(
                            state = state,
                            onStart = { vm.startFast() },
                        )
                    }
                }
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
private fun ChipSlot(state: FastingUiState, displayFasting: Boolean) {
    val c = IntermTheme.colors
    Box(
        modifier = Modifier.defaultMinSize(minHeight = 28.dp),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = displayFasting,
            transitionSpec = {
                fadeIn(tween(durationMillis = 500, easing = FastOutSlowInEasing)) togetherWith
                    fadeOut(tween(durationMillis = 220, easing = FastOutSlowInEasing))
            },
            label = "chip-slot",
        ) { fasting ->
            if (fasting) {
                IntermStageChip(label = "Stage ${state.stageIdx + 1} of ${state.stages.size} · ${state.stage.name}")
            } else {
                Text("NOT FASTING", style = IntermTheme.typography.hEyebrow, color = c.muted)
            }
        }
    }
}

@Composable
private fun ActiveRingCenter(state: FastingUiState) {
    val c = IntermTheme.colors
    val d = TimeMath.fmtDuration(state.elapsedMs)
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

@Composable
private fun IdleRingCenter(state: FastingUiState) {
    val c = IntermTheme.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("GOAL", style = IntermTheme.typography.hEyebrow, color = c.muted)
        Spacer(Modifier.height(6.dp))
        Text("${state.goalHours}h", style = IntermTheme.typography.hDisplay.copy(fontSize = 48.sp), color = c.ink)
        Spacer(Modifier.height(6.dp))
        Text("${state.plan.label} · ${state.plan.subtitle}", style = IntermTheme.typography.caption, color = c.muted)
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
    val dr = TimeMath.fmtDuration(state.remainingMs)
    val startedAt = state.fastStartMs?.let { Instant.ofEpochMilli(it) }
    val goalAt = state.fastEndMs?.let { Instant.ofEpochMilli(it) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
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
            IntermButton(onClick = onEnd, variant = IntermButtonVariant.Soft, modifier = Modifier.weight(1f)) {
                Icon(IntermIcons.Stop, contentDescription = null)
                Text("End Fast")
            }
            IntermButton(onClick = onReset, variant = IntermButtonVariant.Danger, modifier = Modifier.weight(1f)) {
                Icon(IntermIcons.Food, contentDescription = null)
                Text("I Ate")
            }
        }
    }
}

@Composable
private fun IdleBody(
    state: FastingUiState,
    onStart: () -> Unit,
) {
    val c = IntermTheme.colors
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        IntermCard(modifier = Modifier.fillMaxWidth()) {
            Text("READY", style = IntermTheme.typography.hEyebrow, color = c.muted)
            Spacer(Modifier.height(8.dp))
            Text(
                "Ready when you are.",
                style = IntermTheme.typography.headerTitle.copy(fontSize = 18.sp),
                color = c.ink,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Start a ${state.plan.label} fast. We'll track your progress through each metabolic phase.",
                style = IntermTheme.typography.body,
                color = c.ink2,
            )
        }

        IntermButton(onClick = onStart, variant = IntermButtonVariant.Primary, fillWidth = true) {
            Icon(IntermIcons.Play, contentDescription = null)
            Text("Start Fasting")
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
