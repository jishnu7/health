package xyz.jishnu.health.data.repo

import kotlinx.coroutines.flow.Flow
import xyz.jishnu.health.data.local.WeightEntryEntity

interface WeightRepository {
    val allEntries: Flow<List<WeightEntryEntity>>
    val latest: Flow<WeightEntryEntity?>
    fun recent(limit: Int): Flow<List<WeightEntryEntity>>
    fun entriesInRange(fromMs: Long, toMs: Long): Flow<List<WeightEntryEntity>>

    suspend fun findByDay(dayKey: Long): WeightEntryEntity?
    suspend fun upsertForDay(dayKey: Long, weightLb: Double, notes: String? = null)
    suspend fun delete(entry: WeightEntryEntity)
}
