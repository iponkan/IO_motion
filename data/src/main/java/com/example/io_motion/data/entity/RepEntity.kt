package com.example.io_motion.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "rep_entities",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("sessionId")],
)
data class RepEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val repNumber: Int,
    val durationMs: Long,
    val minAngle: Double,
    val maxAngle: Double,
    val qualityScore: Int,
    /** Pipe-separated FormAlert names, e.g. "GO_DEEPER|SAGGING_HIPS". Empty string = no alerts. */
    val alertsPsv: String,
)
