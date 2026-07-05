package com.example.io_motion.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.io_motion.data.dao.SessionDao
import com.example.io_motion.data.entity.RepEntity
import com.example.io_motion.data.entity.SessionEntity

/**
 * Schema is exported to `data/schemas/` (configured via the `room.schemaLocation` KSP arg in
 * `data/build.gradle.kts`) and must be committed to version control — Room diffs against these
 * JSON snapshots to validate hand-written [androidx.room.migration.Migration]s. When [version]
 * changes, a new snapshot is generated on the next build; write a real `Migration` for the bump
 * rather than relying on [com.example.io_motion.data.DataModule]'s destructive fallback, which
 * exists only because there is no prior released version to migrate from yet.
 */
@Database(
    entities = [SessionEntity::class, RepEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
}
