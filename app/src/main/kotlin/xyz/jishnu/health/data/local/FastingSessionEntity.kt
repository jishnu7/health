package xyz.jishnu.health.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fasting_sessions")
data class FastingSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startMs: Long,
    val endMs: Long?,
    val goalHours: Int,
    val planId: String,
    val note: String? = null,
)
