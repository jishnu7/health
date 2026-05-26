package xyz.jishnu.health.data.repo

import kotlinx.coroutines.flow.Flow
import xyz.jishnu.health.data.local.Settings
import xyz.jishnu.health.data.model.Units

interface SettingsRepository {
    val settings: Flow<Settings>
    suspend fun setPlanId(id: String)
    suspend fun setUnits(units: Units)
    suspend fun setFastingReminderOn(on: Boolean)
    suspend fun setWeightReminderOn(on: Boolean)
    suspend fun setWaterReminderOn(on: Boolean)
    suspend fun setStickyNotificationOn(on: Boolean)
    suspend fun setLiveUpdateOn(on: Boolean)
    suspend fun setFastStartTime(hhmm: String)
    suspend fun setReminderTime(hhmm: String)
    suspend fun setDarkMode(on: Boolean)
    suspend fun setOnboarded(on: Boolean)
    suspend fun setWaterGoalMl(ml: Int)
}
