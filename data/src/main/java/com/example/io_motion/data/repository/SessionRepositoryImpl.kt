package com.example.io_motion.data.repository

import com.example.io_motion.core.analysis.model.FormAlert
import com.example.io_motion.core.analysis.model.RepMetrics
import com.example.io_motion.core.analysis.model.SessionMetrics
import com.example.io_motion.core.common.models.AnalysisMode
import com.example.io_motion.core.common.models.ExerciseType
import com.example.io_motion.data.dao.SessionDao
import com.example.io_motion.data.di.ApplicationScope
import com.example.io_motion.data.entity.RepEntity
import com.example.io_motion.data.entity.SessionEntity
import com.example.io_motion.data.entity.SessionWithReps
import com.example.io_motion.data.model.SessionRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

class SessionRepositoryImpl @Inject constructor(
    private val dao: SessionDao,
    @ApplicationScope private val appScope: CoroutineScope,
) : SessionRepository {

    override val sessions: Flow<List<SessionRecord>> =
        dao.observeAll().map { rows -> rows.mapNotNull { it.toRecord() } }

    override fun save(
        metrics: SessionMetrics,
        mode: AnalysisMode,
        modelVariant: String,
        recordedAt: Long,
    ) {
        appScope.launch {
            val sessionId = dao.insertSession(metrics.toEntity(mode, modelVariant, recordedAt))
            dao.insertReps(metrics.reps.map { it.toEntity(sessionId) })
        }
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

/**
 * Returns null (dropping the row) if [SessionEntity.analysisMode] or [SessionEntity.exerciseType]
 * doesn't match a known enum constant — e.g. after a future rename — rather than throwing and
 * crashing every observer of [sessions], including ones showing unrelated sessions.
 */
private fun SessionWithReps.toRecord(): SessionRecord? {
    val analysisMode = runCatching { AnalysisMode.valueOf(session.analysisMode) }.getOrNull() ?: return null
    val exerciseType = runCatching { ExerciseType.valueOf(session.exerciseType) }.getOrNull() ?: return null
    return SessionRecord(
        id = session.id,
        recordedAt = session.recordedAt,
        analysisMode = analysisMode,
        modelVariant = session.modelVariant,
        metrics = SessionMetrics(
            exerciseType = exerciseType,
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
}

private fun RepEntity.toRepMetrics() = RepMetrics(
    repNumber = repNumber,
    durationMs = durationMs,
    minAngle = minAngle,
    maxAngle = maxAngle,
    qualityScore = qualityScore,
    alerts = if (alertsPsv.isBlank()) emptyList()
             else alertsPsv.split("|").mapNotNull { runCatching { FormAlert.valueOf(it) }.getOrNull() },
)
