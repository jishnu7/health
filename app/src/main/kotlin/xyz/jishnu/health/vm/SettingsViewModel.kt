package xyz.jishnu.health.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import xyz.jishnu.health.data.local.Settings
import xyz.jishnu.health.data.model.Units
import xyz.jishnu.health.data.repo.SettingsRepository
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repo: SettingsRepository,
) : ViewModel() {

    val settings: StateFlow<Settings> = repo.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = Settings.Default,
    )

    fun setPlanId(id: String) = viewModelScope.launch { repo.setPlanId(id) }
    fun setUnits(units: Units) = viewModelScope.launch { repo.setUnits(units) }
    fun setFastingReminderOn(on: Boolean) = viewModelScope.launch { repo.setFastingReminderOn(on) }
    fun setWeightReminderOn(on: Boolean) = viewModelScope.launch { repo.setWeightReminderOn(on) }
    fun setStickyNotificationOn(on: Boolean) = viewModelScope.launch { repo.setStickyNotificationOn(on) }
    fun setFastStartTime(hhmm: String) = viewModelScope.launch { repo.setFastStartTime(hhmm) }
    fun setReminderTime(hhmm: String) = viewModelScope.launch { repo.setReminderTime(hhmm) }
}
