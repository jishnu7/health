package xyz.jishnu.health.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import xyz.jishnu.health.data.constants.Plans
import xyz.jishnu.health.data.model.DayEntries
import xyz.jishnu.health.data.model.DayEntry
import xyz.jishnu.health.data.model.Plan
import xyz.jishnu.health.data.model.Units
import xyz.jishnu.health.data.repo.FastingRepository
import xyz.jishnu.health.data.repo.SettingsRepository
import xyz.jishnu.health.data.repo.WeightRepository
import javax.inject.Inject

data class HistoryUiState(
    val entries: List<DayEntry> = emptyList(),
    val plan: Plan = Plans.default,
    val units: Units = Units.Metric,
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val fastingRepo: FastingRepository,
    private val weightRepo: WeightRepository,
    private val settingsRepo: SettingsRepository,
) : ViewModel() {
    val state: StateFlow<HistoryUiState> = combine(
        fastingRepo.allSessions,
        weightRepo.allEntries,
        settingsRepo.settings,
    ) { sessions, weights, settings ->
        HistoryUiState(
            entries = DayEntries.merge(sessions, weights),
            plan = Plans.byId(settings.planId),
            units = settings.units,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())
}
