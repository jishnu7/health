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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import xyz.jishnu.health.data.local.FastingSessionEntity
import xyz.jishnu.health.domain.TimeMath
import xyz.jishnu.health.ui.components.BottomNav
import xyz.jishnu.health.ui.components.CaptureBox
import xyz.jishnu.health.ui.components.EnergyPhaseCard
import xyz.jishnu.health.ui.components.IntermButton
import xyz.jishnu.health.ui.components.IntermButtonSize
import xyz.jishnu.health.ui.components.IntermButtonVariant
import xyz.jishnu.health.ui.components.IntermIcons
import xyz.jishnu.health.ui.components.IntermTopBar
import xyz.jishnu.health.ui.components.LastFastCard
import xyz.jishnu.health.ui.components.LastFastSummary
import xyz.jishnu.health.ui.components.NavTab
import xyz.jishnu.health.ui.components.ProgressRing
import xyz.jishnu.health.ui.components.StagesPreviewCard
import xyz.jishnu.health.ui.components.rememberCardCapture
import xyz.jishnu.health.ui.components.shareBitmapAsImage
import xyz.jishnu.health.ui.theme.IntermTheme
import xyz.jishnu.health.vm.FastingUiState
import xyz.jishnu.health.vm.FastingViewModel
import java.time.Instant

private val RingSize = 250.dp
private val RingStroke = 12.dp
private val DashedRingSize = 240.dp
private val DashedRingStroke = 12.dp

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

    val onStartWithSweep: () -> Unit = {
        scope.launch {
            sweeping = true
            displayFasting = false
            dashAmount.snapTo(1f)
            ringProgress.snapTo(0f)
            ringProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            )
            vm.startFast()
            snapshotFlow { state.isFasting }.first { it }
            displayFasting = true
            ringProgress.snapTo(0f)
            sweeping = false
            dashAmount.animateTo(0f, tween(durationMillis = 350, easing = FastOutSlowInEasing))
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
            displayFasting = false
            dashAmount.snapTo(1f)
            ringProgress.snapTo(1f)
            ringProgress.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            )
            vm.endFast()
            snapshotFlow { state.isFasting }.first { !it }
            sweeping = false
        }
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
            ) {
                Spacer(Modifier.height(16.dp))
                AnimatedContent(
                    targetState = displayFasting,
                    transitionSpec = {
                        fadeIn(tween(durationMillis = 520, easing = FastOutSlowInEasing)) togetherWith
                            fadeOut(tween(durationMillis = 220, easing = FastOutSlowInEasing))
                    },
                    label = "home-state",
                ) { fasting ->
                    if (fasting) {
                        ActiveHomeContent(
                            state = state,
                            ringProgress = ringProgress.value,
                            dashAmount = if (sweeping || !displayFasting) 1f else dashAmount.value,
                            sweeping = sweeping,
                            onOpenStages = onOpenStages,
                            onEnd = onEndWithSweep,
                            onAte = onAteWithSweep,
                        )
                    } else {
                        val last = state.lastFast
                        // The Last Fast card is the user's "I did a meaningful
                        // fast" reward. We only surface it once they actually
                        // reached the 3rd metabolic stage (glycogen burn, ≥8h);
                        // a short last fast falls back to the first-fast layout
                        // with copy nudging them to go longer next time.
                        val thirdStageStartH = state.stages.getOrNull(2)?.startHour ?: 8
                        val lastDurationH = last?.let {
                            ((it.endMs ?: state.nowMs) - it.startMs).coerceAtLeast(0L) / 3_600_000.0
                        } ?: 0.0
                        val showReturning = last != null && lastDurationH >= thirdStageStartH
                        if (showReturning) {
                            ReturningContent(
                                state = state,
                                lastFast = last!!,
                                onStart = onStartWithSweep,
                                onOpenStages = onOpenStages,
                            )
                        } else {
                            FirstFastContent(
                                state = state,
                                ringProgress = ringProgress.value,
                                dashAmount = if (sweeping) 1f else dashAmount.value,
                                onStart = onStartWithSweep,
                                onOpenStages = onOpenStages,
                                hadShortFast = last != null,
                                thirdStageHours = thirdStageStartH,
                            )
                        }
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
private fun FirstFastContent(
    state: FastingUiState,
    ringProgress: Float,
    dashAmount: Float,
    onStart: () -> Unit,
    onOpenStages: () -> Unit,
    /**
     * True when the user *has* fasted before, but the most recent fast was
     * too short to surface as a recap. The layout is the same as the true
     * first-fast state — only the header copy nudges them to go longer.
     */
    hadShortFast: Boolean = false,
    thirdStageHours: Int = 8,
) {
    val c = IntermTheme.colors
    val planStartClock = clockTimeFor(state.fastStartTime)
    val planGoalClock = clockTimeFor(TimeMath.addHoursToTime(state.fastStartTime, state.goalHours.toDouble()))

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        HeaderSection(
            eyebrow = "Not fasting",
            title = if (hadShortFast) "Go a little longer." else "Begin your first fast.",
            body = if (hadShortFast) {
                "Your last fast ended early — aim for at least ${thirdStageHours} hours " +
                    "to reach glycogen burn and unlock the deeper stages."
            } else {
                "We'll track every metabolic stage as you go — from fed to deep ketosis."
            },
        )
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
        StagesPreviewCard(modifier = Modifier.fillMaxWidth(), onOpen = onOpenStages)
    }
}

@Composable
private fun ReturningContent(
    state: FastingUiState,
    lastFast: FastingSessionEntity,
    onStart: () -> Unit,
    onOpenStages: () -> Unit,
) {
    val c = IntermTheme.colors
    val context = LocalContext.current
    val capture = rememberCardCapture()
    val scope = rememberCoroutineScope()

    val endMs = lastFast.endMs ?: state.nowMs
    val durationMs = (endMs - lastFast.startMs).coerceAtLeast(0L)
    val durationH = durationMs / 3_600_000.0
    val summary = LastFastSummary(
        startMs = lastFast.startMs,
        endMs = endMs,
        durationHours = durationH,
        goalHours = lastFast.goalHours,
        planLabel = lastFast.planId,
    )

    val planStartClock = clockTimeFor(state.fastStartTime)
    val planGoalClock = clockTimeFor(TimeMath.addHoursToTime(state.fastStartTime, state.goalHours.toDouble()))

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        HeaderSection(
            eyebrow = "Not fasting",
            title = "Nice work.",
            body = "Here's your last fast. Start another ${state.plan.label} whenever you're ready.",
        )

        // Flip the card into share-image mode for the capture frames so the
        // shared screenshot drops the share button itself and shows the
        // "INTERMITTENT FASTING" eyebrow instead. Reverted as soon as we have
        // the bitmap so the on-screen card keeps its tappable button.
        var capturing by remember { mutableStateOf(false) }
        CaptureBox(capture = capture, modifier = Modifier.fillMaxWidth()) {
            LastFastCard(
                summary = summary,
                modifier = Modifier.fillMaxWidth(),
                forCapture = capturing,
                onShare = {
                    scope.launch {
                        capturing = true
                        androidx.compose.runtime.withFrameNanos { }
                        androidx.compose.runtime.withFrameNanos { }
                        val bitmap = capture.captureBitmap(paddingPx = 48)
                        capturing = false
                        bitmap?.let { shareBitmapAsImage(context, it) }
                    }
                },
            )
        }

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
                Text("Start Fasting now")
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
private fun ActiveHomeContent(
    state: FastingUiState,
    ringProgress: Float,
    dashAmount: Float,
    sweeping: Boolean,
    onOpenStages: () -> Unit,
    onEnd: () -> Unit,
    onAte: () -> Unit,
) {
    val c = IntermTheme.colors
    val d = TimeMath.fmtDuration(state.elapsedMs)
    val dr = TimeMath.fmtDuration(state.remainingMs)
    val startedAt = state.fastStartMs?.let { Instant.ofEpochMilli(it) }
    val goalAt = state.fastEndMs?.let { Instant.ofEpochMilli(it) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        HeaderSection(
            eyebrow = "Fasting · ${state.plan.label}",
            title = "Keep it going.",
            body = "You're ${(state.progress * 100).toInt()}% of the way to your ${state.goalHours}-hour goal — keep it steady.",
        )

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

        EnergyPhaseCard(
            elapsedHours = state.elapsedHours,
            modifier = Modifier.fillMaxWidth(),
            onClick = onOpenStages,
        )
    }
}

@Composable
private fun HeaderSection(eyebrow: String, title: String, body: String) {
    val c = IntermTheme.colors
    Column {
        Text(eyebrow.uppercase(), style = IntermTheme.typography.hEyebrow, color = c.muted)
        Spacer(Modifier.height(8.dp))
        Text(title, style = IntermTheme.typography.headerTitle, color = c.ink)
        Spacer(Modifier.height(8.dp))
        Text(body, style = IntermTheme.typography.body, color = c.ink2)
    }
}

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

