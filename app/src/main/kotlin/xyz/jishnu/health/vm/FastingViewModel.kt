package xyz.jishnu.health.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import xyz.jishnu.health.data.constants.Plans
import xyz.jishnu.health.data.constants.Stages
import xyz.jishnu.health.data.model.Plan
import xyz.jishnu.health.data.model.Stage
import xyz.jishnu.health.data.model.Units
import xyz.jishnu.health.data.repo.FastingRepository
import xyz.jishnu.health.data.repo.SettingsRepository
import xyz.jishnu.health.domain.StageCalculator
import javax.inject.Inject
import kotlin.math.min

data class FastingUiState(
    val isFasting: Boolean,
    val activeSessionId: Long?,
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
class FastingViewModel @Inject constructor(
    private val fastingRepo: FastingRepository,
    private val settingsRepo: SettingsRepository,
) : ViewModel() {

    private val tick = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(1_000)
        }
    }

    val state: StateFlow<FastingUiState> = combine(
        tick,
        fastingRepo.activeSession,
        settingsRepo.settings,
    ) { now, active, settings ->
        val plan = Plans.byId(active?.planId ?: settings.planId)
        FastingUiState(
            isFasting = active != null,
            activeSessionId = active?.id,
            fastStartMs = active?.startMs,
            nowMs = now,
            plan = plan,
            units = settings.units,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FastingUiState(
            isFasting = false,
            activeSessionId = null,
            fastStartMs = null,
            nowMs = System.currentTimeMillis(),
            plan = Plans.default,
            units = Units.Imperial,
        ),
    )

    fun startFast() = viewModelScope.launch {
        val s = settingsRepo.settings.first()
        val plan = Plans.byId(s.planId)
        fastingRepo.startFast(System.currentTimeMillis(), plan.fastHours, plan.id)
    }

    fun endFast() = viewModelScope.launch {
        val active = fastingRepo.activeSession.first() ?: return@launch
        fastingRepo.endFast(active.id, System.currentTimeMillis())
    }

    fun resetFast() = viewModelScope.launch {
        val now = System.currentTimeMillis()
        val active = fastingRepo.activeSession.first()
        active?.let { fastingRepo.endFast(it.id, now) }
        val s = settingsRepo.settings.first()
        val plan = Plans.byId(s.planId)
        fastingRepo.startFast(now, plan.fastHours, plan.id)
    }

    fun setPlan(id: String) = viewModelScope.launch { settingsRepo.setPlanId(id) }
    fun setUnits(units: Units) = viewModelScope.launch { settingsRepo.setUnits(units) }
}
