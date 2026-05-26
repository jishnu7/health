package xyz.jishnu.health.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A single drink logged by the user. Volume is always stored in milliliters. */
@Entity(tableName = "water_entries")
data class WaterEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ml: Int,
    val createdMs: Long,
)
