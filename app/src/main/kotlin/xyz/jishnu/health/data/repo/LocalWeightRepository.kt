package xyz.jishnu.health.data.repo

import kotlinx.coroutines.flow.Flow
import xyz.jishnu.health.data.local.WeightEntryDao
import xyz.jishnu.health.data.local.WeightEntryEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalWeightRepository @Inject constructor(
    private val dao: WeightEntryDao,
) : WeightRepository {
    override val allEntries: Flow<List<WeightEntryEntity>> = dao.observeAll()
    override val latest: Flow<WeightEntryEntity?> = dao.observeLatest()
    override fun recent(limit: Int) = dao.observeRecent(limit)
    override fun entriesInRange(fromMs: Long, toMs: Long) = dao.observeRange(fromMs, toMs)

    override suspend fun findByDay(dayKey: Long): WeightEntryEntity? = dao.findByDayKey(dayKey)

    override suspend fun upsertForDay(dayKey: Long, weightKg: Double, notes: String?) {
        val existing = dao.findByDayKey(dayKey)
        if (existing == null) {
            dao.insert(
                WeightEntryEntity(
                    dayKey = dayKey,
                    weightKg = weightKg,
                    notes = notes,
                    createdMs = System.currentTimeMillis(),
                )
            )
        } else {
            dao.update(existing.copy(weightKg = weightKg, notes = notes))
        }
    }

    override suspend fun delete(entry: WeightEntryEntity) = dao.delete(entry)
}
