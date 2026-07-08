package com.example.io_motion.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val TEST_DB = "migration-test"

@RunWith(AndroidJUnit4::class)
class Migration1To2Test {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
    )

    @Test
    fun migrate1To2_preservesSessionHistory_andCreatesNewTables() {
        // Seed a v1 database with one session row.
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL(
                "INSERT INTO session_entities " +
                    "(id, exerciseType, analysisMode, modelVariant, recordedAt, totalDurationMs, " +
                    "repCount, rejectedRepCount, tempoRpm, rhythmConsistency, avgRomDegrees, " +
                    "sessionQualityScore, validHoldMs, avgBodyLineAngle) " +
                    "VALUES (1, 'SQUAT', 'LIVE', 'FULL', 1000, 60000, 10, 1, 20.0, 90, 95.0, 88, 0, 0.0)"
            )
            close()
        }

        // Run the migration; `true` validates the resulting schema against the exported schema 2.
        val db = helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)

        // Existing session history survives the upgrade.
        db.query("SELECT sessionQualityScore FROM session_entities WHERE id = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(88, cursor.getInt(0))
        }

        // The new v2 tables exist and accept writes (workout item FK -> workout row).
        db.execSQL("INSERT INTO workout_entities (id, name, createdAt, sortOrder) VALUES (1, 'Morning Burn', 0, 0)")
        db.execSQL("INSERT INTO workout_item_entities (id, workoutId, exerciseType, sets, reps, position) VALUES (1, 1, 'SQUAT', 3, 10, 0)")
        db.execSQL("INSERT INTO diet_entry_entities (id, localDate, mealType, name, kcal, loggedAt) VALUES (1, '2026-07-08', 'BREAKFAST', 'Oatmeal & Berries', 310, 0)")
        db.execSQL("INSERT INTO daily_log_entities (localDate, waterCups) VALUES ('2026-07-08', 4)")
        db.close()
    }
}
