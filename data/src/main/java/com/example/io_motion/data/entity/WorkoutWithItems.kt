package com.example.io_motion.data.entity

import androidx.room.Embedded
import androidx.room.Relation

data class WorkoutWithItems(
    @Embedded val workout: WorkoutEntity,
    @Relation(parentColumn = "id", entityColumn = "workoutId")
    val items: List<WorkoutItemEntity>,
)
