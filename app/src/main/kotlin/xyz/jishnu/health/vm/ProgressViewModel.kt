package xyz.jishnu.health.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import xyz.jishnu.health.data.constants.Plans
import xyz.jishnu.health.data.model.DayEntries
import xyz.jishnu.health.data.model.DayEntry
import xyz.jishnu.health.data.model.Plan
import xyz.jishnu.health.data.model.RangeOption
import xyz.jishnu.health.data.model.Units
import xyz.jishnu.health.data.repo.FastingRepository
import xyz.jishnu.health.data.repo.SettingsRepository
import xyz.jishnu.health.data.repo.WeightRepository
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlin.math.abs

data class ProgressUiState(
    val range: RangeOption = RangeOption.Last14,
    val entries: List<DayEntry> = emptyList(),
    val plan: Plan = Plans.default,
    val units: Units = Units.Metric,
    val avgFastHours: Double = 0.0,
    val weightStartLb: Double? = null,
    val weightEndLb: Double? = null,
    val weightChangeLb: Double = 0.0,
    val streakDays: Int = 0,
    val daysGoalMet: Int = 0,
    val dayCount: Int = 0,
)

@HiltViewModel
class ProgressViewModel @Inject constructor(
    private val fastingRepo: FastingRepository,
    private val weightRepo: WeightRepository,
    private val settingsRepo: SettingsRepository,
) : ViewModel() {

    private val selectedRange = MutableStateFlow<RangeOption>(RangeOption.Last14)
    private val pickerOpen = MutableStateFlow(false)

    val state: StateFlow<ProgressUiState> = combine(
        fastingRepo.allSessions,
        weightRepo.allEntries,
        settingsRepo.settings,
        selectedRange,
    ) { sessions, weights, settings, range ->
        val plan = Plans.byId(settings.planId)
        val today = LocalDate.now()
        val (from, to) = range.range(today).let { it.start to it.endInclusive }
        val zone = ZoneId.systemDefault()
        val merged = DayEntries.merge(sessions, weights, zone)
            .filter { it.date >= from && it.date <= to }
            .sortedBy { it.dayKey }

        val firstWeight = merged.firstOrNull { it.weight != null }?.weight?.weightLb
        val lastWeight = merged.lastOrNull { it.weight != null }?.weight?.weightLb
        val change = if (firstWeight != null && lastWeight != null) lastWeight - firstWeight else 0.0
        val streak = computeStreak(merged, plan.fastHours)
        val daysGoalMet = merged.count { it.fastHours >= plan.fastHours }
        // Mean of each day's longest fast — same metric the chart plots.
        val avgFast = if (merged.isEmpty()) 0.0 else merged.sumOf { it.fastHours } / merged.size

        ProgressUiState(
            range = range,
            entries = merged,
            plan = plan,
            units = settings.units,
            avgFastHours = avgFast,
            weightStartLb = firstWeight,
            weightEndLb = lastWeight,
            weightChangeLb = change,
            streakDays = streak,
            daysGoalMet = daysGoalMet,
            dayCount = merged.size,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressUiState())

    val pickerOpenFlow: StateFlow<Boolean> = pickerOpen.let { mf ->
        mf.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    }

    fun setRange(option: RangeOption) { selectedRange.value = option }
    fun setPickerOpen(open: Boolean) { pickerOpen.value = open }
    fun togglePicker() { pickerOpen.value = !pickerOpen.value }

    private fun computeStreak(entries: List<DayEntry>, goal: Int): Int {
        var streak = 0
        for (e in entries.reversed()) {
            if (e.fastHours >= goal) streak++ else break
        }
        return streak
    }
}
