package com.example.io_motion.core.common.models

enum class ExerciseType {
    SQUAT,
    SIT_UP,
    PUSH_UP,
    PLANK,
}

/** User-facing label, previously duplicated identically in feature-video and feature-history. */
fun ExerciseType.displayName(): String = when (this) {
    ExerciseType.SQUAT   -> "Squat"
    ExerciseType.PUSH_UP -> "Push-up"
    ExerciseType.SIT_UP  -> "Sit-up"
    ExerciseType.PLANK   -> "Plank"
}
