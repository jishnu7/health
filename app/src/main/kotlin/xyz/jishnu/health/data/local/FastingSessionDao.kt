package xyz.jishnu.health.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FastingSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: FastingSessionEntity): Long

    @Update
    suspend fun update(session: FastingSessionEntity)

    @Delete
    suspend fun delete(session: FastingSessionEntity)

    @Query("UPDATE fasting_sessions SET endMs = :endMs WHERE id = :id")
    suspend fun setEnd(id: Long, endMs: Long)

    @Query("SELECT * FROM fasting_sessions WHERE endMs IS NULL ORDER BY startMs DESC LIMIT 1")
    fun observeActive(): Flow<FastingSessionEntity?>

    @Query("SELECT * FROM fasting_sessions ORDER BY startMs DESC")
    fun observeAll(): Flow<List<FastingSessionEntity>>

    @Query("SELECT * FROM fasting_sessions WHERE startMs >= :fromMs AND startMs < :toMs ORDER BY startMs DESC")
    fun observeRange(fromMs: Long, toMs: Long): Flow<List<FastingSessionEntity>>

    @Query("SELECT * FROM fasting_sessions WHERE endMs >= :fromMs AND endMs < :toMs ORDER BY startMs DESC")
    fun observeEndingInRange(fromMs: Long, toMs: Long): Flow<List<FastingSessionEntity>>

    @Query("SELECT * FROM fasting_sessions WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): FastingSessionEntity?
}
