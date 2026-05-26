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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.first
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
    val dashAmount = remember { Animatable(if (state.isFasting) 0f else 1f) }
    var sweeping by remember { mutableStateOf(false) }
    // A locally-driven mirror of `state.isFasting`. We hold it back until the
    // sweep peaks so the center / chip / body crossfades line up with the ring
    // refilling from zero, instead of fading on top of a still-sweeping ring.
    var displayFasting by remember { mutableStateOf(state.isFasting) }
    // "I ate" reuses resetFast which fires the start-fast clockwise sweep via
    // the LaunchedEffect on state.fastStartMs. This one-shot flag lets the
    // handler tell that effect to skip the next change because it owns the
    // animation itself.
    var suppressNextStartSweep by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Whenever the underlying state changes outside of a sweep (process restart,
    // resetFast, …) catch the display state up so the two never get stuck out
    // of sync.
    LaunchedEffect(state.isFasting, sweeping) {
        if (!sweeping) displayFasting = state.isFasting
    }

    LaunchedEffect(state.fastStartMs) {
        if (state.fastStartMs == null) return@LaunchedEffect
        if (suppressNextStartSweep) {
            suppressNextStartSweep = false
            return@LaunchedEffect
        }
        sweeping = true
        displayFasting = false
        // Mirror the I-ate / end-fast motion: snap to a full dashed ring and
        // drain anti-clockwise. The new active state is revealed at the bottom
        // of the wipe so the chip and numbers settle in with the ring fill.
        dashAmount.snapTo(1f)
        ringProgress.snapTo(1f)
        ringProgress.animateTo(0f, tween(durationMillis = 900, easing = FastOutSlowInEasing))
        displayFasting = true
        sweeping = false
        // Close the dash gaps and fill back up to the live progress in parallel
        // so the dashed→solid transition is part of the same motion.
        scope.launch {
            dashAmount.animateTo(0f, tween(durationMillis = 350, easing = FastOutSlowInEasing))
        }
        ringProgress.animateTo(
            targetValue = state.progress,
            animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        )
    }

    LaunchedEffect(state.progress, sweeping, state.isFasting, displayFasting) {
        if (!sweeping && state.isFasting && displayFasting) {
            ringProgress.animateTo(
                targetValue = state.progress,
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
            )
        } else if (!state.isFasting && !sweeping) {
            ringProgress.snapTo(0f)
        }
    }

    val onAteWithSweep: () -> Unit = {
        scope.launch {
            sweeping = true
            // "I ate" stays in the active visual the whole time — only the
            // ring drains anti-clockwise. The text content (stage chip, ring
            // center, action buttons) doesn't fade because the next state is
            // also a running fast.
            dashAmount.snapTo(1f)
            ringProgress.snapTo(1f)
            ringProgress.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            )
            val prevStartMs = state.fastStartMs
            suppressNextStartSweep = true
            vm.resetFast()
            // Wait for the new fast's startMs to land so the progress mirror
            // doesn't briefly animate against stale data once sweeping clears.
            snapshotFlow { state.fastStartMs }.first { it != null && it != prevStartMs }
            sweeping = false
            // Close the dash gaps smoothly so the ring eases back to its
            // solid resting look instead of snapping.
            dashAmount.animateTo(0f, tween(durationMillis = 350, easing = FastOutSlowInEasing))
        }
    }

    val onEndWithSweep: () -> Unit = {
        scope.launch {
            sweeping = true
            // Reveal the idle state as the ring drains. Snap to a full ring
            // first so the anti-clockwise wipe always covers the entire loop
            // regardless of how much real progress was on the dial.
            displayFasting = false
            dashAmount.snapTo(1f)
            ringProgress.snapTo(1f)
            ringProgress.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            )
            vm.endFast()
            // Wait until the VM's StateFlow has actually settled to !isFasting
            // before releasing the sweep — otherwise the mirror effect on
            // (state.isFasting, sweeping) briefly bounces displayFasting back
            // to true and the ring flashes a frame of fill.
            snapshotFlow { state.isFasting }.first { !it }
            sweeping = false
            // Idle state keeps the dashed look — no animation needed.
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
                    dashAmount = if (sweeping || !displayFasting) 1f else dashAmount.value,
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
                            onReset = onAteWithSweep,
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
