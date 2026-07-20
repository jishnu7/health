package xyz.jishnu.health.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [FastingSessionEntity::class, WeightEntryEntity::class, WaterEntryEntity::class],
    version = 4,
    exportSchema = false,
)
abstract class IntermDatabase : RoomDatabase() {
    abstract fun fastingSessionDao(): FastingSessionDao
    abstract fun weightEntryDao(): WeightEntryDao
    abstract fun waterEntryDao(): WaterEntryDao
}

/**
 * One-time cleanup: earlier builds persisted the placeholder default weight
 * (70.0 kg) for days the user never actually weighed — e.g. saving a day's fast
 * would also write the stepper's default weight. Those fake readings drag the
 * weight trend toward 70. Drop them so the carry-forward display fills the gap
 * instead. Data-only migration; no schema change.
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DELETE FROM weight_entries WHERE ABS(weightKg - 70.0) < 0.005")
    }
}
