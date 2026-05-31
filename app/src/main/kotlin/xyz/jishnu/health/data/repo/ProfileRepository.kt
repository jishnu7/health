package xyz.jishnu.health.data.repo

import kotlinx.coroutines.flow.Flow
import xyz.jishnu.health.data.local.Profile
import xyz.jishnu.health.data.model.Sex

interface ProfileRepository {
    val profile: Flow<Profile>
    suspend fun setSex(sex: Sex?)
    suspend fun setHeightCm(cm: Double?)
    suspend fun setDateOfBirth(iso: String?)
}
