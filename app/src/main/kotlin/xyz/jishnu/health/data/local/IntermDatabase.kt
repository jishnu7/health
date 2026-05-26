package xyz.jishnu.health.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [FastingSessionEntity::class, WeightEntryEntity::class, WaterEntryEntity::class],
    version = 3,
    exportSchema = false,
)
abstract class IntermDatabase : RoomDatabase() {
    abstract fun fastingSessionDao(): FastingSessionDao
    abstract fun weightEntryDao(): WeightEntryDao
    abstract fun waterEntryDao(): WaterEntryDao
}
