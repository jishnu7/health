package xyz.jishnu.health.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WaterEntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: WaterEntryEntity): Long

    @Delete
    suspend fun delete(entry: WaterEntryEntity)

    @Query("DELETE FROM water_entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM water_entries WHERE createdMs >= :fromMs AND createdMs < :toMs ORDER BY createdMs ASC")
    fun observeRange(fromMs: Long, toMs: Long): Flow<List<WaterEntryEntity>>

    @Query("SELECT COALESCE(SUM(ml), 0) FROM water_entries WHERE createdMs >= :fromMs AND createdMs < :toMs")
    fun observeRangeTotal(fromMs: Long, toMs: Long): Flow<Int>
}
