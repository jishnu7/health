package xyz.jishnu.health.vm

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import xyz.jishnu.health.data.local.Settings
import xyz.jishnu.health.data.model.Units
import xyz.jishnu.health.data.repo.FastingRepository
import xyz.jishnu.health.data.repo.SettingsRepository
import xyz.jishnu.health.notifications.FastingForegroundService
import xyz.jishnu.health.notifications.ReminderScheduler
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repo: SettingsRepository,
    private val fastingRepo: FastingRepository,
    private val scheduler: ReminderScheduler,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    val settings: StateFlow<Settings> = repo.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = Settings.Default,
    )

    fun setPlanId(id: String) = viewModelScope.launch { repo.setPlanId(id) }
    fun setUnits(units: Units) = viewModelScope.launch { repo.setUnits(units) }

    fun setFastingReminderOn(on: Boolean) = viewModelScope.launch {
        repo.setFastingReminderOn(on)
        scheduler.syncFromSettings(appContext, repo.settings.first())
    }
    fun setWeightReminderOn(on: Boolean) = viewModelScope.launch {
        repo.setWeightReminderOn(on)
        scheduler.syncFromSettings(appContext, repo.settings.first())
    }
    fun setWaterReminderOn(on: Boolean) = viewModelScope.launch {
        repo.setWaterReminderOn(on)
        scheduler.syncFromSettings(appContext, repo.settings.first())
    }
    fun setStickyNotificationOn(on: Boolean) = viewModelScope.launch {
        repo.setStickyNotificationOn(on)
        // Sync the foreground service immediately so the sticky notification
        // disappears as soon as the toggle goes off (and comes back if it's
        // flipped on again during an active fast).
        val active = fastingRepo.activeSession.first()
        if (on) {
            if (active != null) FastingForegroundService.start(appContext)
        } else {
            FastingForegroundService.stop(appContext)
        }
    }
    fun setLiveUpdateOn(on: Boolean) = viewModelScope.launch {
        // The foreground service observes this flag and re-posts itself, so we
        // don't need to start/stop here — only when sticky changes.
        repo.setLiveUpdateOn(on)
    }
    fun setFastStartTime(hhmm: String) = viewModelScope.launch {
        repo.setFastStartTime(hhmm)
        scheduler.syncFromSettings(appContext, repo.settings.first())
    }
    fun setReminderTime(hhmm: String) = viewModelScope.launch {
        repo.setReminderTime(hhmm)
        scheduler.syncFromSettings(appContext, repo.settings.first())
    }
    fun markOnboarded() = viewModelScope.launch { repo.setOnboarded(true) }
    fun setWaterGoalMl(ml: Int) = viewModelScope.launch { repo.setWaterGoalMl(ml) }
}
