package com.example.io_motion.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "session_entities")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val exerciseType: String,
    val analysisMode: String,
    val modelVariant: String,
    val recordedAt: Long,
    val totalDurationMs: Long,
    val repCount: Int,
    val rejectedRepCount: Int,
    val tempoRpm: Double,
    val rhythmConsistency: Int,
    val avgRomDegrees: Double,
    val sessionQualityScore: Int,
    val validHoldMs: Long,
    val avgBodyLineAngle: Double,
)
