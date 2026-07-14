package com.example.io_motion.data

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * One-shot demo seeder. [onCreate] fires exactly once, when the database file is first created, so
 * this never re-seeds and never touches an existing (real) user database. Registered in
 * [DataModule] only for `BuildConfig.DEBUG` builds, so no demo data ships in release.
 *
 * Ids are hard-coded (1, 2, …) safely: the tables are guaranteed empty inside [onCreate], so
 * AUTOINCREMENT assigns them in order.
 */
internal class DebugSeedCallback : RoomDatabase.Callback() {

    override fun onCreate(db: SupportSQLiteDatabase) {
        val now = System.currentTimeMillis()

        // "Morning Burn" — Squat ×3, Plank ×3 (plank reps column stores seconds of hold).
        db.execSQL("INSERT INTO workout_entities (name, createdAt, sortOrder) VALUES ('Morning Burn', $now, 0)")
        db.execSQL("INSERT INTO workout_item_entities (workoutId, exerciseType, sets, reps, position) VALUES (1, 'SQUAT', 3, 10, 0)")
        db.execSQL("INSERT INTO workout_item_entities (workoutId, exerciseType, sets, reps, position) VALUES (1, 'PLANK', 3, 30, 1)")

        // "Core Focus" — Sit-up ×4, Plank ×3.
        db.execSQL("INSERT INTO workout_entities (name, createdAt, sortOrder) VALUES ('Core Focus', ${now + 1}, 1)")
        db.execSQL("INSERT INTO workout_item_entities (workoutId, exerciseType, sets, reps, position) VALUES (2, 'SIT_UP', 4, 10, 0)")
        db.execSQL("INSERT INTO workout_item_entities (workoutId, exerciseType, sets, reps, position) VALUES (2, 'PLANK', 3, 30, 1)")

        // Diet: today's sample meals (design §8). Date is computed with SimpleDateFormat (device
        // zone) to avoid a java.time desugaring dependency in :data; it matches DietMath.dateKey's
        // ISO yyyy-MM-dd format so the diet screen keys onto these on the install day.
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(now))
        db.execSQL("INSERT INTO diet_entry_entities (localDate, mealType, name, kcal, loggedAt) VALUES ('$today', 'BREAKFAST', 'Oatmeal & Berries', 310, $now)")
        db.execSQL("INSERT INTO diet_entry_entities (localDate, mealType, name, kcal, loggedAt) VALUES ('$today', 'LUNCH', 'Grilled Chicken Bowl', 420, $now)")
        db.execSQL("INSERT INTO diet_entry_entities (localDate, mealType, name, kcal, loggedAt) VALUES ('$today', 'SNACKS', 'Greek Yogurt', 150, $now)")
    }
}
