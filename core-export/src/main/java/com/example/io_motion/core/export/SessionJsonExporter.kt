package com.example.io_motion.core.export

import com.example.io_motion.core.analysis.model.SessionMetrics
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Serializes a session to a structured JSON string suitable for backend ingestion or archiving.
 *
 * Private DTOs carry only primitive types so no @Serializable annotation is needed
 * on domain classes in other modules.
 */
object SessionJsonExporter {

    private val json = Json { prettyPrint = true; encodeDefaults = true }

    fun export(
        id: Long,
        recordedAt: Long,
        analysisMode: String,
        modelVariant: String,
        metrics: SessionMetrics,
    ): String = json.encodeToString(metrics.toDto(id, recordedAt, analysisMode, modelVariant))
}

// ── DTOs ──────────────────────────────────────────────────────────────────────

@Serializable
private data class SessionDto(
    val id: Long,
    val recordedAt: Long,
    val exerciseType: String,
    val analysisMode: String,
    val modelVariant: String,
    val totalDurationMs: Long,
    val repCount: Int,
    val rejectedRepCount: Int,
    val tempoRpm: Double,
    val rhythmConsistency: Int,
    val avgRomDegrees: Double,
    val sessionQualityScore: Int,
    val validHoldMs: Long,
    val avgBodyLineAngle: Double,
    val reps: List<RepDto>,
)

@Serializable
private data class RepDto(
    val repNumber: Int,
    val durationMs: Long,
    val minAngle: Double,
    val maxAngle: Double,
    val romDegrees: Double,
    val qualityScore: Int,
    val alerts: List<String>,
)

// ── Mapper ────────────────────────────────────────────────────────────────────

private fun SessionMetrics.toDto(
    id: Long,
    recordedAt: Long,
    analysisMode: String,
    modelVariant: String,
) = SessionDto(
    id = id,
    recordedAt = recordedAt,
    exerciseType = exerciseType.name,
    analysisMode = analysisMode,
    modelVariant = modelVariant,
    totalDurationMs = totalDurationMs,
    repCount = repCount,
    rejectedRepCount = rejectedRepCount,
    tempoRpm = tempoRpm,
    rhythmConsistency = rhythmConsistency,
    avgRomDegrees = avgRomDegrees,
    sessionQualityScore = sessionQualityScore,
    validHoldMs = validHoldMs,
    avgBodyLineAngle = avgBodyLineAngle,
    reps = reps.map { rep ->
        RepDto(
            repNumber = rep.repNumber,
            durationMs = rep.durationMs,
            minAngle = rep.minAngle,
            maxAngle = rep.maxAngle,
            romDegrees = rep.rom,
            qualityScore = rep.qualityScore,
            alerts = rep.alerts.map { it.name },
        )
    },
)
