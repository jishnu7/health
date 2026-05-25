package xyz.jishnu.health.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [FastingSessionEntity::class, WeightEntryEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class IntermDatabase : RoomDatabase() {
    abstract fun fastingSessionDao(): FastingSessionDao
    abstract fun weightEntryDao(): WeightEntryDao
}
