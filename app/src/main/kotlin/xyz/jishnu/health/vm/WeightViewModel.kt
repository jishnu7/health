package xyz.jishnu.health.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import xyz.jishnu.health.data.local.WeightEntryEntity
import xyz.jishnu.health.data.model.DayEntries
import xyz.jishnu.health.data.model.Units
import xyz.jishnu.health.data.repo.SettingsRepository
import xyz.jishnu.health.data.repo.WeightRepository
import javax.inject.Inject

data class WeightUiState(
    val units: Units = Units.Imperial,
    val previous: WeightEntryEntity? = null,
    val draftLb: Double? = null,
    val recent: List<WeightEntryEntity> = emptyList(),
) {
    val sevenDayAverageLb: Double? = recent.takeIf { it.isNotEmpty() }
        ?.let { list -> list.take(7).map { it.weightLb }.average() }
}

@HiltViewModel
class WeightViewModel @Inject constructor(
    private val repo: WeightRepository,
    private val settingsRepo: SettingsRepository,
) : ViewModel() {

    private val draftLb = MutableStateFlow<Double?>(null)

    val state: StateFlow<WeightUiState> = combine(
        repo.recent(7),
        settingsRepo.settings,
        draftLb.asStateFlow(),
    ) { recent, settings, draft ->
        val previous = recent.firstOrNull()
        WeightUiState(
            units = settings.units,
            previous = previous,
            draftLb = draft ?: previous?.weightLb ?: 150.0,
            recent = recent,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = WeightUiState(),
    )

    fun setDraft(lb: Double) { draftLb.value = lb }
    fun bumpDraft(delta: Double) {
        draftLb.value = (draftLb.value ?: state.value.previous?.weightLb ?: 150.0) + delta
    }

    fun save(notes: String? = null, onSaved: () -> Unit = {}) = viewModelScope.launch {
        val lb = draftLb.value ?: state.value.previous?.weightLb ?: return@launch
        val today = DayEntries.dayKeyFor(System.currentTimeMillis())
        repo.upsertForDay(today, lb, notes)
        onSaved()
    }
}
