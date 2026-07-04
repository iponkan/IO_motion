package com.example.io_motion.data.entity

import androidx.room.Embedded
import androidx.room.Relation

data class SessionWithReps(
    @Embedded val session: SessionEntity,
    @Relation(parentColumn = "id", entityColumn = "sessionId")
    val reps: List<RepEntity>,
)
