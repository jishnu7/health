package xyz.jishnu.health.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import xyz.jishnu.health.data.constants.Plans
import xyz.jishnu.health.data.constants.Stages
import xyz.jishnu.health.data.model.Plan
import xyz.jishnu.health.data.model.Stage
import xyz.jishnu.health.data.model.Units
import xyz.jishnu.health.domain.StageCalculator
import javax.inject.Inject
import kotlin.math.min

data class FastingUiState(
    val isFasting: Boolean,
    val fastStartMs: Long?,
    val nowMs: Long,
    val plan: Plan,
    val units: Units,
    val stages: List<Stage> = Stages.all,
) {
    val elapsedMs: Long = if (isFasting && fastStartMs != null) (nowMs - fastStartMs).coerceAtLeast(0L) else 0L
    val elapsedHours: Double = elapsedMs / 3_600_000.0
    val goalHours: Int = plan.fastHours
    val goalMs: Long = goalHours * 3_600_000L
    val progress: Float = min(1.0, elapsedHours / goalHours).toFloat()
    val stage: Stage = StageCalculator.stageFor(elapsedHours, stages)
    val stageIdx: Int = stages.indexOf(stage)
    val remainingMs: Long = (goalMs - elapsedMs).coerceAtLeast(0L)
    val fastEndMs: Long? = fastStartMs?.let { it + goalMs }
}

@HiltViewModel
class FastingViewModel @Inject constructor() : ViewModel() {

    private val fastStartMs = MutableStateFlow<Long?>(null)
    private val isFasting = MutableStateFlow(false)
    private val plan = MutableStateFlow(Plans.default)
    private val units = MutableStateFlow(Units.Imperial)

    private val tick = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(1_000)
        }
    }

    val state: StateFlow<FastingUiState> = combine(
        tick, isFasting, fastStartMs, plan, units,
    ) { now, fasting, startMs, p, u ->
        FastingUiState(
            isFasting = fasting,
            fastStartMs = startMs,
            nowMs = now,
            plan = p,
            units = u,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FastingUiState(
            isFasting = false,
            fastStartMs = null,
            nowMs = System.currentTimeMillis(),
            plan = Plans.default,
            units = Units.Imperial,
        ),
    )

    fun startFast() {
        fastStartMs.value = System.currentTimeMillis()
        isFasting.value = true
    }

    fun endFast() {
        isFasting.value = false
    }

    fun resetFast() {
        fastStartMs.value = System.currentTimeMillis()
        isFasting.value = true
    }

    fun setPlan(id: String) {
        plan.value = Plans.byId(id)
    }

    fun setUnits(u: Units) {
        units.value = u
    }
}
