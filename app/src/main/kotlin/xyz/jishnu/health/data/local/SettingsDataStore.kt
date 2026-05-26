package xyz.jishnu.health.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import xyz.jishnu.health.data.model.Units

data class Settings(
    val planId: String,
    val units: Units,
    val fastingReminderOn: Boolean,
    val weightReminderOn: Boolean,
    val waterReminderOn: Boolean,
    val stickyNotificationOn: Boolean,
    val fastStartTime: String,
    val reminderTime: String,
    val darkMode: Boolean,
    val onboarded: Boolean,
    val waterGoalMl: Int,
) {
    companion object {
        val Default = Settings(
            planId = "16:8",
            units = Units.Metric,
            fastingReminderOn = true,
            weightReminderOn = true,
            waterReminderOn = true,
            stickyNotificationOn = true,
            fastStartTime = "20:00",
            reminderTime = "07:30",
            darkMode = false,
            onboarded = false,
            waterGoalMl = 2500,
        )
    }
}

class SettingsDataStore(private val dataStore: DataStore<Preferences>) {

    val settings: Flow<Settings> = dataStore.data.map { p ->
        Settings(
            planId = p[Keys.PlanId] ?: Settings.Default.planId,
            units = (p[Keys.Units] ?: Settings.Default.units.name).let { runCatching { Units.valueOf(it) }.getOrDefault(Units.Metric) },
            fastingReminderOn = p[Keys.FastingReminderOn] ?: Settings.Default.fastingReminderOn,
            weightReminderOn = p[Keys.WeightReminderOn] ?: Settings.Default.weightReminderOn,
            waterReminderOn = p[Keys.WaterReminderOn] ?: Settings.Default.waterReminderOn,
            stickyNotificationOn = p[Keys.StickyNotificationOn] ?: Settings.Default.stickyNotificationOn,
            fastStartTime = p[Keys.FastStartTime] ?: Settings.Default.fastStartTime,
            reminderTime = p[Keys.ReminderTime] ?: Settings.Default.reminderTime,
            darkMode = p[Keys.DarkMode] ?: Settings.Default.darkMode,
            onboarded = p[Keys.Onboarded] ?: Settings.Default.onboarded,
            waterGoalMl = p[Keys.WaterGoalMl] ?: Settings.Default.waterGoalMl,
        )
    }

    suspend fun setPlanId(id: String) = edit { it[Keys.PlanId] = id }
    suspend fun setUnits(units: Units) = edit { it[Keys.Units] = units.name }
    suspend fun setFastingReminderOn(on: Boolean) = edit { it[Keys.FastingReminderOn] = on }
    suspend fun setWeightReminderOn(on: Boolean) = edit { it[Keys.WeightReminderOn] = on }
    suspend fun setWaterReminderOn(on: Boolean) = edit { it[Keys.WaterReminderOn] = on }
    suspend fun setStickyNotificationOn(on: Boolean) = edit { it[Keys.StickyNotificationOn] = on }
    suspend fun setFastStartTime(value: String) = edit { it[Keys.FastStartTime] = value }
    suspend fun setReminderTime(value: String) = edit { it[Keys.ReminderTime] = value }
    suspend fun setDarkMode(on: Boolean) = edit { it[Keys.DarkMode] = on }
    suspend fun setOnboarded(on: Boolean) = edit { it[Keys.Onboarded] = on }
    suspend fun setWaterGoalMl(ml: Int) = edit { it[Keys.WaterGoalMl] = ml }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        dataStore.edit { prefs -> block(prefs) }
    }

    private object Keys {
        val PlanId = stringPreferencesKey("plan_id")
        val Units = stringPreferencesKey("units")
        val FastingReminderOn = booleanPreferencesKey("fasting_reminder_on")
        val WeightReminderOn = booleanPreferencesKey("weight_reminder_on")
        val WaterReminderOn = booleanPreferencesKey("water_reminder_on")
        val StickyNotificationOn = booleanPreferencesKey("sticky_notification_on")
        val FastStartTime = stringPreferencesKey("fast_start_time")
        val ReminderTime = stringPreferencesKey("reminder_time")
        val DarkMode = booleanPreferencesKey("dark_mode")
        val Onboarded = booleanPreferencesKey("onboarded")
        val WaterGoalMl = intPreferencesKey("water_goal_ml")
    }
}
