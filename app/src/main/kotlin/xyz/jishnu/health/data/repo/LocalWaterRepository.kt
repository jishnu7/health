package xyz.jishnu.health.data.repo

import kotlinx.coroutines.flow.Flow
import xyz.jishnu.health.data.local.WaterEntryDao
import xyz.jishnu.health.data.local.WaterEntryEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalWaterRepository @Inject constructor(
    private val dao: WaterEntryDao,
) : WaterRepository {
    override fun entriesInRange(fromMs: Long, toMs: Long): Flow<List<WaterEntryEntity>> =
        dao.observeRange(fromMs, toMs)

    override fun totalInRange(fromMs: Long, toMs: Long): Flow<Int> =
        dao.observeRangeTotal(fromMs, toMs)

    override suspend fun addWater(ml: Int, atMs: Long): Long =
        dao.insert(WaterEntryEntity(ml = ml, createdMs = atMs))

    override suspend fun delete(entry: WaterEntryEntity) = dao.delete(entry)
    override suspend fun deleteById(id: Long) = dao.deleteById(id)
}
