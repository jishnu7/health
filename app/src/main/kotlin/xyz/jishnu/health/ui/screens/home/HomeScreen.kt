package xyz.jishnu.health.ui.screens.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import xyz.jishnu.health.data.local.FastingSessionEntity
import xyz.jishnu.health.domain.TimeMath
import xyz.jishnu.health.ui.components.BottomNav
import xyz.jishnu.health.ui.components.HiddenLastFastCaptureCard
import xyz.jishnu.health.ui.components.IntermButton
import xyz.jishnu.health.ui.components.IntermButtonSize
import xyz.jishnu.health.ui.components.IntermButtonVariant
import xyz.jishnu.health.ui.components.IntermIcons
import xyz.jishnu.health.ui.components.IntermTopBar
import xyz.jishnu.health.ui.components.LastFastSummary
import xyz.jishnu.health.ui.components.MetabolicStageCard
import xyz.jishnu.health.ui.components.NavTab
import xyz.jishnu.health.ui.components.ProgressRing
import xyz.jishnu.health.ui.components.ShareableLastFastCard
import xyz.jishnu.health.ui.components.StagesPreviewCard
import xyz.jishnu.health.ui.components.rememberCardCapture
import xyz.jishnu.health.ui.components.rememberFastShareTrigger
import xyz.jishnu.health.ui.theme.IntermTheme
import xyz.jishnu.health.vm.FastingUiState
import xyz.jishnu.health.vm.FastingViewModel
import java.time.Instant

private val RingSize = 250.dp
private val RingStroke = 12.dp
private val DashedRingSize = 240.dp
private val DashedRingStroke = 12.dp

/**
 * Total duration of the Y-axis flip used for the share-card ↔ active-card
 * handoff. Split evenly across the two halves of the rotation.
 */
private const val FlipDurationMillis: Int = 640

/**
 * Overlap windows between the ring-swipe and the card flip so the two motions
 * blend instead of running back-to-back. Start fast = the spin begins this
 * many ms before the flip finishes; end fast = the flip begins this many ms
 * before the swipe finishes.
 */
private const val FlipSpinOverlapMillis: Int = 100
private const val SwipeFlipOverlapMillis: Int = 180

/** Ring-swipe / drain animation length used by start- and end-fast sweeps. */
private const val RingSwipeDurationMillis: Int = 900

/**
 * Top-card "phase" that decides which composable goes into the flip slot.
 * Only [Returning] ↔ [Active] transitions actually animate the flip — the
 * first-fast top card (which holds the Start button) doesn't flip per design.
 */
private enum class TopPhase { FirstFast, Returning, Active }

private enum class FlipDirection { Clockwise, AntiClockwise }

@Composable
fun HomeScreen(
    vm: FastingViewModel,
    onNavigateTab: (NavTab) -> Unit,
    onOpenStages: () -> Unit,
    onOpenSettings: () -> Unit,
    /**
     * Monotonic counter — the value increments every time something external
     * (e.g. DayDetail's Resume) wants Home to replay its start-fast animation.
     * Using a counter instead of a one-shot Boolean means the prop never flips
     * mid-animation, so the LaunchedEffect below isn't cancelled half-way.
     */
    animateStartNonce: Long = 0L,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val c = IntermTheme.colors

    val ringProgress = remember { Animatable(if (state.isFasting) state.progress else 0f) }
    val dashAmount = remember { Animatable(if (state.isFasting) 0f else 1f) }
    var sweeping by remember { mutableStateOf(false) }
    var displayFasting by remember { mutableStateOf(state.isFasting) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(state.isFasting, sweeping) {
        if (!sweeping && displayFasting != state.isFasting) {
            displayFasting = state.isFasting
            if (!state.isFasting) {
                ringProgress.snapTo(0f)
                dashAmount.snapTo(1f)
            }
        }
    }

    LaunchedEffect(state.progress, sweeping, state.isFasting, displayFasting) {
        if (!sweeping && state.isFasting && displayFasting) {
            val diff = kotlin.math.abs(state.progress - ringProgress.value)
            if (diff > 0.05f) {
                ringProgress.snapTo(state.progress)
            } else {
                ringProgress.animateTo(
                    targetValue = state.progress,
                    animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
                )
            }
        }
    }

    var lastHandledStartNonce by remember { mutableStateOf(animateStartNonce) }
    LaunchedEffect(animateStartNonce) {
        if (animateStartNonce == lastHandledStartNonce) return@LaunchedEffect
        lastHandledStartNonce = animateStartNonce
        snapshotFlow { state.isFasting }.first { it }
        sweeping = true
        displayFasting = false
        dashAmount.snapTo(1f)
        ringProgress.snapTo(0f)
        ringProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        )
        displayFasting = true
        ringProgress.snapTo(0f)
        sweeping = false
        scope.launch { dashAmount.animateTo(0f, tween(durationMillis = 350, easing = FastOutSlowInEasing)) }
        ringProgress.animateTo(
            targetValue = state.progress,
            animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        )
    }

    // The Last Fast card is the user's "I did a meaningful fast" reward. We
    // only surface it once they actually reached the 3rd metabolic stage
    // (glycogen burn, ≥8 h); a shorter last fast falls back to the first-fast
    // layout so the user is nudged to push longer rather than rewarded for
    // a short one.
    val thirdStageStartH = state.stages.getOrNull(2)?.startHour ?: 8
    val lastDurationH = state.lastFast?.let {
        ((it.endMs ?: state.nowMs) - it.startMs).coerceAtLeast(0L) / 3_600_000.0
    } ?: 0.0
    val qualifyingReturning = state.lastFast != null && lastDurationH >= thirdStageStartH

    val onStartWithSweep: () -> Unit = {
        scope.launch {
            // Only the qualifying-returning path flips — a short last fast
            // shows the first-fast layout, which doesn't flip.
            val fromReturning = qualifyingReturning
            sweeping = true
            displayFasting = false
            if (fromReturning) {
                // Share card → active ring card: container grows to the
                // active card's height first, THEN flips (anti-clockwise),
                // and the ring spin kicks in while the flip is still landing
                // (last [FlipSpinOverlapMillis] of the rotation) so the two
                // motions hand off seamlessly instead of running back-to-back.
                vm.startFast()
                snapshotFlow { state.isFasting }.first { it }
                displayFasting = true
                delay(
                    (SizeAnimationDurationMillis + FlipDurationMillis - FlipSpinOverlapMillis)
                        .toLong()
                        .coerceAtLeast(0L),
                )
                dashAmount.snapTo(1f)
                ringProgress.snapTo(0f)
                ringProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = RingSwipeDurationMillis, easing = FastOutSlowInEasing),
                )
                sweeping = false
                scope.launch { dashAmount.animateTo(0f, tween(durationMillis = 450, easing = FastOutSlowInEasing)) }
                ringProgress.animateTo(
                    targetValue = state.progress,
                    animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
                )
            } else {
                // First-fast: no flip (dashed-ring card holds the Start button
                // and "stays as it is" per design). Keep the wipe-then-settle
                // pattern.
                dashAmount.snapTo(1f)
                ringProgress.snapTo(0f)
                ringProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
                )
                vm.startFast()
                snapshotFlow { state.isFasting }.first { it }
                displayFasting = true
                sweeping = false
                scope.launch { dashAmount.animateTo(0f, tween(durationMillis = 450, easing = FastOutSlowInEasing)) }
                ringProgress.animateTo(
                    targetValue = state.progress,
                    animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
                )
            }
        }
    }

    val onAteWithSweep: () -> Unit = {
        scope.launch {
            sweeping = true
            dashAmount.snapTo(1f)
            ringProgress.snapTo(1f)
            ringProgress.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            )
            val prevStartMs = state.fastStartMs
            vm.resetFast()
            snapshotFlow { state.fastStartMs }.first { it != null && it != prevStartMs }
            sweeping = false
            dashAmount.animateTo(0f, tween(durationMillis = 350, easing = FastOutSlowInEasing))
        }
    }

    val onEndWithSweep: () -> Unit = {
        scope.launch {
            sweeping = true
            // Drain the ring on the still-visible active card, but begin the
            // clockwise flip while the last ~20% of the swipe is still
            // running ([SwipeFlipOverlapMillis] of overlap). The card starts
            // rotating away as the ring finishes draining — one continuous
            // motion instead of two phases stacked back-to-back.
            dashAmount.snapTo(1f)
            ringProgress.snapTo(1f)
            val drainJob = launch {
                ringProgress.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = RingSwipeDurationMillis, easing = FastOutSlowInEasing),
                )
            }
            // Backend update can fire in parallel — `sweeping = true` gates
            // the displayFasting mirror so it won't race us.
            vm.endFast()
            delay((RingSwipeDurationMillis - SwipeFlipOverlapMillis).toLong().coerceAtLeast(0L))
            displayFasting = false
            drainJob.join()
            delay((FlipDurationMillis - SwipeFlipOverlapMillis).toLong().coerceAtLeast(0L))
            snapshotFlow { state.isFasting }.first { !it }
            sweeping = false
        }
    }

    val topPhase = when {
        displayFasting -> TopPhase.Active
        qualifyingReturning -> TopPhase.Returning
        else -> TopPhase.FirstFast
    }

    Box(modifier = Modifier.fillMaxSize().background(c.bg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            IntermTopBar(
                modifier = Modifier.statusBarsPadding(),
                title = "Fast",
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
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Spacer(Modifier.height(0.dp))

                // Top card slot — flip only on Returning ↔ Active transitions.
                FlipContent(
                    targetState = topPhase,
                    durationMillis = FlipDurationMillis,
                    shouldFlip = { from, to ->
                        (from == TopPhase.Returning && to == TopPhase.Active) ||
                            (from == TopPhase.Active && to == TopPhase.Returning)
                    },
                    flipDirection = { _, to ->
                        if (to == TopPhase.Active) FlipDirection.AntiClockwise else FlipDirection.Clockwise
                    },
                    // Going to the active card (taller): grow the container
                    // first so the flip lands at the final size. Going the
                    // other way: let animateContentSize shrink naturally
                    // after the flip completes.
                    growBeforeFlip = { _, to -> to == TopPhase.Active },
                ) { phase ->
                    when (phase) {
                        TopPhase.Active -> ActiveTopCard(
                            state = state,
                            ringProgress = ringProgress.value,
                            dashAmount = if (sweeping || !displayFasting) 1f else dashAmount.value,
                            onEnd = onEndWithSweep,
                            onAte = onAteWithSweep,
                        )
                        TopPhase.Returning -> ReturningTopCard(
                            state = state,
                            lastFast = state.lastFast!!,
                        )
                        TopPhase.FirstFast -> FirstFastTopCard(
                            state = state,
                            ringProgress = ringProgress.value,
                            dashAmount = if (sweeping) 1f else dashAmount.value,
                            onStart = onStartWithSweep,
                        )
                    }
                }

                // Lower content slot — the Start-button "Next fast" card and
                // its peers grow / shrink with a fade so they smoothly appear
                // and disappear instead of popping out under the flip card.
                AnimatedContent(
                    targetState = topPhase,
                    transitionSpec = {
                        (fadeIn(tween(durationMillis = 420, easing = FastOutSlowInEasing)) +
                            expandVertically(
                                animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
                                expandFrom = Alignment.Top,
                            )) togetherWith
                            (fadeOut(tween(durationMillis = 240, easing = FastOutSlowInEasing)) +
                                shrinkVertically(
                                    animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
                                    shrinkTowards = Alignment.Top,
                                ))
                    },
                    label = "home-lower",
                ) { phase ->
                    when (phase) {
                        TopPhase.Active -> ActiveLowerContent(state = state, onOpenStages = onOpenStages)
                        TopPhase.Returning -> ReturningLowerContent(
                            state = state,
                            onStart = onStartWithSweep,
                            onOpenStages = onOpenStages,
                        )
                        TopPhase.FirstFast -> FirstFastLowerContent(onOpenStages = onOpenStages)
                    }
                }

                Spacer(Modifier.height(4.dp))
            }
            BottomNav(
                active = NavTab.Today,
                onChange = onNavigateTab,
                modifier = Modifier.navigationBarsPadding(),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────
// Top card slots — these render the card immediately under the top bar.
// ─────────────────────────────────────────────────────────────────────────

@Composable
private fun ReturningTopCard(state: FastingUiState, lastFast: FastingSessionEntity) {
    val endMs = lastFast.endMs ?: state.nowMs
    val durationMs = (endMs - lastFast.startMs).coerceAtLeast(0L)
    val summary = LastFastSummary(
        startMs = lastFast.startMs,
        endMs = endMs,
        durationHours = durationMs / 3_600_000.0,
        goalHours = lastFast.goalHours,
        planLabel = lastFast.planId,
    )
    ShareableLastFastCard(summary = summary, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun ActiveTopCard(
    state: FastingUiState,
    ringProgress: Float,
    dashAmount: Float,
    onEnd: () -> Unit,
    onAte: () -> Unit,
) {
    val c = IntermTheme.colors
    val d = TimeMath.fmtDuration(state.elapsedMs)
    val dr = TimeMath.fmtDuration(state.remainingMs)
    val startedAt = state.fastStartMs?.let { Instant.ofEpochMilli(it) }
    val goalAt = state.fastEndMs?.let { Instant.ofEpochMilli(it) }

    // Hidden capture for sharing the in-progress fast as a snapshot. The
    // summary is rebuilt each tick from the live state so a tap on Share
    // captures the latest pixels.
    val capture = rememberCardCapture()
    val ongoingSummary = remember(state.fastStartMs, state.nowMs, state.goalHours, state.plan.label) {
        val startMs = state.fastStartMs ?: state.nowMs
        LastFastSummary(
            startMs = startMs,
            endMs = state.nowMs,
            durationHours = state.elapsedHours,
            goalHours = state.goalHours,
            planLabel = state.plan.label,
            isOngoing = true,
        )
    }
    val share = rememberFastShareTrigger(capture, chooserTitle = "Share progress")

    Box(modifier = Modifier.fillMaxWidth()) {
        HiddenLastFastCaptureCard(summary = ongoingSummary, capture = capture)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(c.card)
                .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ProgressRing(
                progress = ringProgress,
                size = RingSize,
                stroke = RingStroke,
                dashAmount = dashAmount,
            ) {
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
            Spacer(Modifier.height(22.dp))
            Divider()
            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TimeMarker("Started", startedAt?.let { TimeMath.fmtTime(it) } ?: "—", Alignment.Start)
                TimeMarker("Remaining", "${dr.hours}h ${dr.mm}m", Alignment.CenterHorizontally)
                TimeMarker("Goal", goalAt?.let { TimeMath.fmtTime(it) } ?: "—", Alignment.End)
            }
            Spacer(Modifier.height(18.dp))
            Divider()
            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                IntermButton(
                    onClick = onEnd,
                    variant = IntermButtonVariant.Soft,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(IntermIcons.Stop, contentDescription = null)
                    Text("End Fast")
                }
                IntermButton(
                    onClick = onAte,
                    variant = IntermButtonVariant.Danger,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(IntermIcons.Food, contentDescription = null)
                    Text("I Ate")
                }
            }
        }
        // Share only becomes available once the user has actually crossed
        // into the 2nd metabolic stage — sharing a 30-minute "in progress"
        // snapshot isn't meaningful, but once stage 2 lands there's a real
        // milestone to share.
        val phaseTwoStartH = state.stages.getOrNull(1)?.startHour ?: 4
        if (state.elapsedHours >= phaseTwoStartH) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 14.dp, end = 14.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(c.primarySoft)
                    .clickable(onClick = share),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    IntermIcons.Share,
                    contentDescription = "Share progress",
                    tint = c.primary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun FirstFastTopCard(
    state: FastingUiState,
    ringProgress: Float,
    dashAmount: Float,
    onStart: () -> Unit,
) {
    val c = IntermTheme.colors
    val planStartClock = clockTimeFor(state.fastStartTime)
    val planGoalClock = clockTimeFor(TimeMath.addHoursToTime(state.fastStartTime, state.goalHours.toDouble()))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(c.card)
            .padding(start = 20.dp, end = 20.dp, top = 30.dp, bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ProgressRing(
            progress = ringProgress,
            size = DashedRingSize,
            stroke = DashedRingStroke,
            dashAmount = dashAmount,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("GOAL", style = IntermTheme.typography.hEyebrow, color = c.muted)
                Spacer(Modifier.height(6.dp))
                Text(
                    "${state.goalHours}h",
                    style = IntermTheme.typography.hDisplay.copy(fontSize = 48.sp),
                    color = c.ink,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "${state.plan.label} Plan",
                    style = IntermTheme.typography.caption,
                    color = c.muted,
                )
            }
        }
        Spacer(Modifier.height(22.dp))
        Divider()
        Spacer(Modifier.height(18.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            CaptionPair("Starts", planStartClock, Alignment.Start)
            CaptionPair("Goal", planGoalClock, Alignment.End)
        }
        Spacer(Modifier.height(18.dp))
        Divider()
        Spacer(Modifier.height(18.dp))
        IntermButton(
            onClick = onStart,
            variant = IntermButtonVariant.Primary,
            size = IntermButtonSize.Large,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(IntermIcons.Play, contentDescription = null)
            Text("Start Fasting")
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────
// Lower content slots — crossfade between these as the top phase changes.
// ─────────────────────────────────────────────────────────────────────────

@Composable
private fun ReturningLowerContent(
    state: FastingUiState,
    onStart: () -> Unit,
    onOpenStages: () -> Unit,
) {
    val c = IntermTheme.colors
    val planStartClock = clockTimeFor(state.fastStartTime)
    val planGoalClock = clockTimeFor(TimeMath.addHoursToTime(state.fastStartTime, state.goalHours.toDouble()))

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(c.card)
                .padding(18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("NEXT FAST", style = IntermTheme.typography.hEyebrow, color = c.muted)
                Text(
                    "${state.goalHours}h",
                    style = IntermTheme.typography.hDisplay.copy(fontSize = 26.sp),
                    color = c.ink,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "${state.goalHours}-hour fast, ${24 - state.goalHours}-hour eating window.",
                style = IntermTheme.typography.body,
                color = c.muted,
            )
            Spacer(Modifier.height(16.dp))
            IntermButton(
                onClick = onStart,
                variant = IntermButtonVariant.Primary,
                size = IntermButtonSize.Large,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(IntermIcons.Play, contentDescription = null)
                Text("Start Fasting Now")
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    IntermIcons.Clock,
                    contentDescription = null,
                    tint = c.muted,
                    modifier = Modifier.size(13.dp),
                )
                Spacer(Modifier.size(6.dp))
                Text(
                    "Usual window · $planStartClock – $planGoalClock",
                    style = IntermTheme.typography.caption,
                    color = c.muted,
                )
            }
        }
        StagesPreviewCard(modifier = Modifier.fillMaxWidth(), onOpen = onOpenStages)
    }
}

@Composable
private fun ActiveLowerContent(state: FastingUiState, onOpenStages: () -> Unit) {
    MetabolicStageCard(
        elapsedHours = state.elapsedHours,
        modifier = Modifier.fillMaxWidth(),
        onClick = onOpenStages,
    )
}

@Composable
private fun FirstFastLowerContent(onOpenStages: () -> Unit) {
    StagesPreviewCard(modifier = Modifier.fillMaxWidth(), onOpen = onOpenStages)
}

// ─────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────

@Composable
private fun CaptionPair(label: String, value: String, align: Alignment.Horizontal) {
    val c = IntermTheme.colors
    Column(horizontalAlignment = align) {
        Text(label, style = IntermTheme.typography.caption, color = c.muted)
        Spacer(Modifier.height(2.dp))
        Text(
            value,
            style = IntermTheme.typography.mono.copy(fontSize = 14.sp, fontWeight = FontWeight.W500),
            color = c.ink,
        )
    }
}

@Composable
private fun TimeMarker(label: String, value: String, align: Alignment.Horizontal) {
    val c = IntermTheme.colors
    Column(horizontalAlignment = align) {
        Text(label, style = IntermTheme.typography.caption, color = c.muted)
        Spacer(Modifier.height(2.dp))
        Text(
            value,
            style = IntermTheme.typography.mono.copy(fontSize = 14.sp, fontWeight = FontWeight.W500),
            color = c.ink,
        )
    }
}

/**
 * Y-axis flip between two pieces of content with selective animation,
 * directional rotation, and smooth size transitions to keep neighbouring
 * layout from jumping mid-flip.
 *
 * - [shouldFlip] gates the rotation per (from → to) transition; when false
 *   the content swaps instantly. Used to keep the first-fast top card from
 *   flipping.
 * - [flipDirection] maps "start fast = anti-clockwise / end fast = clockwise"
 *   to the sign of the rotation.
 * - [growBeforeFlip] decides when the target content's *size* should be
 *   reserved BEFORE the rotation starts. The container grows to the larger
 *   size first, then flips inside it (no jump). When false, the size is
 *   allowed to animate naturally after the flip — the shrink-after pattern.
 */
@Composable
private fun <T> FlipContent(
    targetState: T,
    durationMillis: Int,
    modifier: Modifier = Modifier,
    shouldFlip: (from: T, to: T) -> Boolean = { _, _ -> true },
    flipDirection: (from: T, to: T) -> FlipDirection = { _, _ -> FlipDirection.AntiClockwise },
    growBeforeFlip: (from: T, to: T) -> Boolean = { _, _ -> false },
    content: @Composable (T) -> Unit,
) {
    var displayed by remember { mutableStateOf(targetState) }
    var sizeReservation by remember { mutableStateOf<T?>(null) }
    val rotation = remember { Animatable(0f) }

    LaunchedEffect(targetState) {
        if (targetState == displayed) return@LaunchedEffect
        val from = displayed
        val to = targetState
        try {
            if (shouldFlip(from, to)) {
                if (growBeforeFlip(from, to)) {
                    // Pre-render the target invisibly so the container grows
                    // to the larger size via animateContentSize BEFORE the
                    // rotation starts. The wait gives that size animation
                    // room to land first.
                    sizeReservation = to
                    delay(SizeAnimationDurationMillis.toLong())
                }
                val sign = if (flipDirection(from, to) == FlipDirection.AntiClockwise) 1f else -1f
                val half = durationMillis / 2
                rotation.animateTo(
                    targetValue = 90f * sign,
                    animationSpec = tween(durationMillis = half, easing = FastOutLinearInEasing),
                )
                displayed = to
                rotation.snapTo(-90f * sign)
                rotation.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = half, easing = LinearOutSlowInEasing),
                )
            } else {
                displayed = to
                rotation.snapTo(0f)
            }
        } finally {
            // Whether we completed or got cancelled, the reservation has
            // served its purpose. Clearing it doesn't shrink anything because
            // `displayed` now equals the reserved state.
            sizeReservation = null
        }
    }

    Box(
        modifier = modifier.animateContentSize(
            animationSpec = tween(
                durationMillis = SizeAnimationDurationMillis,
                easing = FastOutSlowInEasing,
            ),
        ),
    ) {
        // Invisible space-reserver during a grow-before-flip transition. Its
        // intrinsic size pushes the parent's animateContentSize, which grows
        // the container to fit the new face before the rotation begins.
        sizeReservation?.takeIf { it != displayed }?.let { reserved ->
            Box(modifier = Modifier.alpha(0f)) {
                content(reserved)
            }
        }
        // Visible face — the only piece that rotates.
        Box(
            modifier = Modifier.graphicsLayer {
                rotationY = rotation.value
                cameraDistance = 24f * density
            },
        ) {
            content(displayed)
        }
    }
}

private const val SizeAnimationDurationMillis: Int = 320

@Composable
private fun Divider() {
    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(IntermTheme.colors.border))
}

private fun clockTimeFor(hhmm: String): String {
    val (h, m) = hhmm.split(":").map { it.toInt() }
    val instant = java.time.LocalDate.now()
        .atTime(h, m)
        .atZone(java.time.ZoneId.systemDefault())
        .toInstant()
    return TimeMath.fmtTime(instant)
}
