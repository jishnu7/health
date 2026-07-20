package xyz.jishnu.health.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import xyz.jishnu.health.data.local.WeightEntryEntity
import xyz.jishnu.health.data.model.DayEntries
import xyz.jishnu.health.data.model.Sex
import xyz.jishnu.health.data.model.Units
import xyz.jishnu.health.data.repo.ProfileRepository
import xyz.jishnu.health.data.repo.SettingsRepository
import xyz.jishnu.health.data.repo.WeightRepository
import javax.inject.Inject

data class WeightUiState(
    val units: Units = Units.Metric,
    val previous: WeightEntryEntity? = null,
    val draftKg: Double? = null,
    val recent: List<WeightEntryEntity> = emptyList(),
    val sex: Sex? = null,
    val heightCm: Double? = null,
) {
    val sevenDayAverageKg: Double? = recent.takeIf { it.isNotEmpty() }
        ?.let { list -> list.take(7).map { it.weightKg }.average() }
}

@HiltViewModel
class WeightViewModel @Inject constructor(
    private val repo: WeightRepository,
    private val settingsRepo: SettingsRepository,
    private val profileRepo: ProfileRepository,
) : ViewModel() {

    // Default starting weight when no previous entry exists. 70 kg is a reasonable
    // mid-range placeholder; the user adjusts from here.
    private val defaultKg = 70.0

    private val draftKg = MutableStateFlow<Double?>(null)

    val state: StateFlow<WeightUiState> = combine(
        repo.recent(56),
        settingsRepo.settings,
        profileRepo.profile,
        draftKg.asStateFlow(),
    ) { recent, settings, profile, draft ->
        val todayKey = DayEntries.dayKeyFor(System.currentTimeMillis())
        val todayEntry = recent.firstOrNull { it.dayKey == todayKey }
        // "Previous" — the baseline for the Change readout — is the most recent
        // reading from a day *other than today*, so re-saving or editing today's
        // weight is compared against the prior reading, never against itself.
        val previous = recent.firstOrNull { it.dayKey != todayKey }
        WeightUiState(
            units = settings.units,
            previous = previous,
            draftKg = draft ?: todayEntry?.weightKg ?: previous?.weightKg ?: defaultKg,
            recent = recent,
            sex = profile.sex,
            heightCm = profile.heightCm,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = WeightUiState(),
    )

    fun setDraftKg(kg: Double) { draftKg.value = kg }
    fun bumpDraftKg(deltaKg: Double) {
        // Bump from the value currently shown in the stepper (today's reading if
        // logged, else the last recorded weight), not the Change baseline.
        draftKg.value = (state.value.draftKg ?: defaultKg) + deltaKg
    }

    fun save(notes: String? = null, onSaved: () -> Unit = {}) = viewModelScope.launch {
        val today = DayEntries.dayKeyFor(System.currentTimeMillis())
        val kg = draftKg.value
            ?: repo.findByDay(today)?.weightKg      // keep today's own value if untouched
            ?: state.value.previous?.weightKg        // else carry the last recorded weight
            ?: return@launch                          // nothing to base a reading on
        repo.upsertForDay(today, kg, notes)
        onSaved()
    }
}
