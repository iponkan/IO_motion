package com.example.io_motion.data.model

import com.example.io_motion.core.common.models.ExerciseType

/** Public view of a saved custom workout and its ordered routine items. */
data class Workout(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val sortOrder: Int,
    val items: List<WorkoutItem>,
)

data class WorkoutItem(
    val id: Long,
    val exerciseType: ExerciseType,
    val sets: Int,
    /** Reps per set; for [ExerciseType.PLANK] this value is seconds of hold. */
    val reps: Int,
    val position: Int,
)
