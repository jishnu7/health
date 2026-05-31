package xyz.jishnu.health.data.repo

import xyz.jishnu.health.data.local.ProfileDataStore
import xyz.jishnu.health.data.model.Sex
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStoreProfileRepository @Inject constructor(
    private val store: ProfileDataStore,
) : ProfileRepository {
    override val profile = store.profile
    override suspend fun setSex(sex: Sex?) = store.setSex(sex)
    override suspend fun setHeightCm(cm: Double?) = store.setHeightCm(cm)
    override suspend fun setDateOfBirth(iso: String?) = store.setDateOfBirth(iso)
}
