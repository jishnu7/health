package xyz.jishnu.health.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import xyz.jishnu.health.data.model.Sex

/**
 * The user's biographical profile. Lives in its own DataStore so identity
 * data is decoupled from app preferences (units, reminders, plan, …).
 */
data class Profile(
    val sex: Sex?,
    val heightCm: Double?,
    val dateOfBirth: String?,
) {
    companion object {
        val Empty = Profile(sex = null, heightCm = null, dateOfBirth = null)
    }
}

class ProfileDataStore(private val dataStore: DataStore<Preferences>) {

    val profile: Flow<Profile> = dataStore.data.map { p ->
        Profile(
            sex = p[Keys.Sex]?.let { runCatching { Sex.valueOf(it) }.getOrNull() },
            heightCm = p[Keys.HeightCm],
            dateOfBirth = p[Keys.DateOfBirth],
        )
    }

    suspend fun setSex(value: Sex?) = edit {
        if (value == null) it.remove(Keys.Sex) else it[Keys.Sex] = value.name
    }
    suspend fun setHeightCm(value: Double?) = edit {
        if (value == null) it.remove(Keys.HeightCm) else it[Keys.HeightCm] = value
    }
    suspend fun setDateOfBirth(value: String?) = edit {
        if (value.isNullOrBlank()) it.remove(Keys.DateOfBirth) else it[Keys.DateOfBirth] = value
    }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        dataStore.edit { prefs -> block(prefs) }
    }

    private object Keys {
        val Sex = stringPreferencesKey("sex")
        val HeightCm = doublePreferencesKey("height_cm")
        val DateOfBirth = stringPreferencesKey("date_of_birth")
    }
}
