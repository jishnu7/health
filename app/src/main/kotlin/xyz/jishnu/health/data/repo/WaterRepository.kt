package xyz.jishnu.health.data.repo

import kotlinx.coroutines.flow.Flow
import xyz.jishnu.health.data.local.WaterEntryEntity

interface WaterRepository {
    fun entriesInRange(fromMs: Long, toMs: Long): Flow<List<WaterEntryEntity>>
    fun totalInRange(fromMs: Long, toMs: Long): Flow<Int>
    suspend fun addWater(ml: Int, atMs: Long = System.currentTimeMillis()): Long
    suspend fun delete(entry: WaterEntryEntity)
    suspend fun deleteById(id: Long)
}
