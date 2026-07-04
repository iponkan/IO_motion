package com.example.io_motion.data.repository

import com.example.io_motion.core.analysis.model.FormAlert
import com.example.io_motion.core.analysis.model.RepMetrics
import com.example.io_motion.core.analysis.model.SessionMetrics
import com.example.io_motion.core.common.models.AnalysisMode
import com.example.io_motion.core.common.models.ExerciseType
import com.example.io_motion.data.dao.SessionDao
import com.example.io_motion.data.entity.RepEntity
import com.example.io_motion.data.entity.SessionEntity
import com.example.io_motion.data.entity.SessionWithReps
import com.example.io_motion.data.model.SessionRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SessionRepositoryImpl @Inject constructor(
    private val dao: SessionDao,
) : SessionRepository {

    override val sessions: Flow<List<SessionRecord>> =
        dao.observeAll().map { rows -> rows.map { it.toRecord() } }

    override suspend fun save(
        metrics: SessionMetrics,
        mode: AnalysisMode,
        modelVariant: String,
        recordedAt: Long,
    ): Long {
        val sessionId = dao.insertSession(metrics.toEntity(mode, modelVariant, recordedAt))
        dao.insertReps(metrics.reps.map { it.toEntity(sessionId) })
        return sessionId
    }

    override suspend fun getById(id: Long): SessionRecord? =
        dao.getById(id)?.toRecord()

    override suspend fun delete(id: Long) {
        dao.deleteById(id)
    }
}

// ── Mappers ────────────────────────────────────────────────────────────────────

private fun SessionMetrics.toEntity(
    mode: AnalysisMode,
    modelVariant: String,
    recordedAt: Long,
) = SessionEntity(
    exerciseType = exerciseType.name,
    analysisMode = mode.name,
    modelVariant = modelVariant,
    recordedAt = recordedAt,
    totalDurationMs = totalDurationMs,
    repCount = repCount,
    rejectedRepCount = rejectedRepCount,
    tempoRpm = tempoRpm,
    rhythmConsistency = rhythmConsistency,
    avgRomDegrees = avgRomDegrees,
    sessionQualityScore = sessionQualityScore,
    validHoldMs = validHoldMs,
    avgBodyLineAngle = avgBodyLineAngle,
)

private fun RepMetrics.toEntity(sessionId: Long) = RepEntity(
    sessionId = sessionId,
    repNumber = repNumber,
    durationMs = durationMs,
    minAngle = minAngle,
    maxAngle = maxAngle,
    qualityScore = qualityScore,
    alertsPsv = alerts.joinToString("|") { it.name },
)

private fun SessionWithReps.toRecord() = SessionRecord(
    id = session.id,
    recordedAt = session.recordedAt,
    analysisMode = AnalysisMode.valueOf(session.analysisMode),
    modelVariant = session.modelVariant,
    metrics = SessionMetrics(
        exerciseType = ExerciseType.valueOf(session.exerciseType),
        totalDurationMs = session.totalDurationMs,
        repCount = session.repCount,
        rejectedRepCount = session.rejectedRepCount,
        tempoRpm = session.tempoRpm,
        rhythmConsistency = session.rhythmConsistency,
        avgRomDegrees = session.avgRomDegrees,
        sessionQualityScore = session.sessionQualityScore,
        reps = reps.sortedBy { it.repNumber }.map { it.toRepMetrics() },
        validHoldMs = session.validHoldMs,
        avgBodyLineAngle = session.avgBodyLineAngle,
    ),
)

private fun RepEntity.toRepMetrics() = RepMetrics(
    repNumber = repNumber,
    durationMs = durationMs,
    minAngle = minAngle,
    maxAngle = maxAngle,
    qualityScore = qualityScore,
    alerts = if (alertsPsv.isBlank()) emptyList()
             else alertsPsv.split("|").mapNotNull { runCatching { FormAlert.valueOf(it) }.getOrNull() },
)
