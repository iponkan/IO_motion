package com.example.io_motion.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.io_motion.data.dao.DietDao
import com.example.io_motion.data.dao.SessionDao
import com.example.io_motion.data.dao.WorkoutDao
import com.example.io_motion.data.entity.DailyLogEntity
import com.example.io_motion.data.entity.DietEntryEntity
import com.example.io_motion.data.entity.RepEntity
import com.example.io_motion.data.entity.SessionEntity
import com.example.io_motion.data.entity.WorkoutEntity
import com.example.io_motion.data.entity.WorkoutItemEntity

/**
 * Schema is exported to `data/schemas/` (configured via the `room.schemaLocation` KSP arg in
 * `data/build.gradle.kts`) and must be committed to version control — Room diffs against these
 * JSON snapshots to validate hand-written [androidx.room.migration.Migration]s.
 *
 * v1 shipped in 0.1.0; v2 adds the workout + diet tables (see [MIGRATION_1_2]). Every future
 * version bump must ship a real `Migration` registered in [DataModule] — the destructive fallback
 * has been removed, so an unmigrated bump now fails loudly instead of silently wiping user data.
 */
@Database(
    entities = [
        SessionEntity::class,
        RepEntity::class,
        WorkoutEntity::class,
        WorkoutItemEntity::class,
        DietEntryEntity::class,
        DailyLogEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun dietDao(): DietDao
}
