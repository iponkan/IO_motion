package com.example.io_motion.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workout_item_entities",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("workoutId")],
)
data class WorkoutItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutId: Long,
    /** [com.example.io_motion.core.common.models.ExerciseType] name; same enum-string convention as SessionEntity. */
    val exerciseType: String,
    val sets: Int,
    /** Reps per set — for PLANK this is interpreted as seconds of hold (see the builder/runner). */
    val reps: Int,
    val position: Int,
)
