package com.example.io_motion.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v1 → v2: adds the workout (Create Your Workout) and diet (Diet Planning) tables. Purely additive
 * — the existing `session_entities` / `rep_entities` are untouched, so all session history survives
 * the upgrade. The raw SQL below must match the generated v2 schema (`data/schemas/…/2.json`)
 * exactly; Room validates the resulting schema when the database is opened and in the
 * MigrationTestHelper androidTest.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `workout_entities` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, " +
                "`createdAt` INTEGER NOT NULL, " +
                "`sortOrder` INTEGER NOT NULL)"
        )

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `workout_item_entities` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`workoutId` INTEGER NOT NULL, " +
                "`exerciseType` TEXT NOT NULL, " +
                "`sets` INTEGER NOT NULL, " +
                "`reps` INTEGER NOT NULL, " +
                "`position` INTEGER NOT NULL, " +
                "FOREIGN KEY(`workoutId`) REFERENCES `workout_entities`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_workout_item_entities_workoutId` " +
                "ON `workout_item_entities` (`workoutId`)"
        )

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `diet_entry_entities` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`localDate` TEXT NOT NULL, " +
                "`mealType` TEXT NOT NULL, " +
                "`name` TEXT NOT NULL, " +
                "`kcal` INTEGER NOT NULL, " +
                "`loggedAt` INTEGER NOT NULL)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_diet_entry_entities_localDate` " +
                "ON `diet_entry_entities` (`localDate`)"
        )

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `daily_log_entities` (" +
                "`localDate` TEXT NOT NULL, " +
                "`waterCups` INTEGER NOT NULL, " +
                "PRIMARY KEY(`localDate`))"
        )
    }
}
