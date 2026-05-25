package xyz.jishnu.health.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weight_entries")
data class WeightEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dayKey: Long,
    val weightLb: Double,
    val notes: String? = null,
    val createdMs: Long,
)
