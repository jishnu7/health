package xyz.jishnu.health.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import xyz.jishnu.health.data.local.WaterEntryEntity
import xyz.jishnu.health.data.model.DayEntries
import xyz.jishnu.health.data.model.Units
import xyz.jishnu.health.data.repo.SettingsRepository
import xyz.jishnu.health.data.repo.WaterRepository
import javax.inject.Inject
import kotlin.math.min

data class WaterUiState(
    val units: Units = Units.Metric,
    val totalMl: Int = 0,
    val goalMl: Int = 2500,
    val log: List<WaterEntryEntity> = emptyList(),
) {
    val progress: Float = if (goalMl <= 0) 0f else min(1.0, totalMl.toDouble() / goalMl).toFloat()
    val remainingMl: Int = (goalMl - totalMl).coerceAtLeast(0)
}

@HiltViewModel
class WaterViewModel @Inject constructor(
    private val waterRepo: WaterRepository,
    private val settingsRepo: SettingsRepository,
) : ViewModel() {

    // Re-emit the [dayStart, dayEnd) window once a minute so the list/total roll over
    // at midnight without the user reopening the screen.
    private val dayWindow = flow {
        while (true) {
            val start = DayEntries.dayKeyFor(System.currentTimeMillis())
            emit(start to (start + 86_400_000L))
            delay(60_000)
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val state: StateFlow<WaterUiState> = dayWindow.flatMapLatest { (fromMs, toMs) ->
        combine(
            waterRepo.entriesInRange(fromMs, toMs),
            waterRepo.totalInRange(fromMs, toMs),
            settingsRepo.settings,
        ) { log, total, settings ->
            WaterUiState(
                units = settings.units,
                totalMl = total,
                goalMl = settings.waterGoalMl,
                log = log,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = WaterUiState(),
    )

    fun addWater(ml: Int) = viewModelScope.launch {
        if (ml > 0) waterRepo.addWater(ml)
    }

    fun removeEntry(entry: WaterEntryEntity) = viewModelScope.launch {
        waterRepo.delete(entry)
    }

    fun setGoal(ml: Int) = viewModelScope.launch {
        if (ml > 0) settingsRepo.setWaterGoalMl(ml)
    }
}
