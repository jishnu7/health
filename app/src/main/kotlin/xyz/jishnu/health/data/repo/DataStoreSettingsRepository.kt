package xyz.jishnu.health.data.repo

import xyz.jishnu.health.data.local.Settings
import xyz.jishnu.health.data.local.SettingsDataStore
import xyz.jishnu.health.data.model.Units
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStoreSettingsRepository @Inject constructor(
    private val store: SettingsDataStore,
) : SettingsRepository {
    override val settings = store.settings
    override suspend fun setPlanId(id: String) = store.setPlanId(id)
    override suspend fun setUnits(units: Units) = store.setUnits(units)
    override suspend fun setFastingReminderOn(on: Boolean) = store.setFastingReminderOn(on)
    override suspend fun setWeightReminderOn(on: Boolean) = store.setWeightReminderOn(on)
    override suspend fun setStickyNotificationOn(on: Boolean) = store.setStickyNotificationOn(on)
    override suspend fun setFastStartTime(hhmm: String) = store.setFastStartTime(hhmm)
    override suspend fun setReminderTime(hhmm: String) = store.setReminderTime(hhmm)
    override suspend fun setDarkMode(on: Boolean) = store.setDarkMode(on)
    override suspend fun setOnboarded(on: Boolean) = store.setOnboarded(on)

    @Suppress("unused") private val _settings: Settings = Settings.Default
}
