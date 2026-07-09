package com.example.io_motion.data.model

import com.example.io_motion.core.analysis.model.SessionMetrics
import com.example.io_motion.core.common.models.AnalysisMode
import com.example.io_motion.core.common.models.ExerciseType

/** Public view of a persisted session, combining storage metadata with computed metrics. */
data class SessionRecord(
    val id: Long,
    val recordedAt: Long,
    val analysisMode: AnalysisMode,
    val modelVariant: String,
    val metrics: SessionMetrics,
) {
    /**
     * A handful of metric fields are surfaced directly so consumers that only need these numbers
     * (the Home hub stats, the guided-workout runner in `:feature-workout`) don't have to depend on
     * `:core-analysis` for the [SessionMetrics] type.
     */
    val qualityScore: Int get() = metrics.sessionQualityScore
    val exerciseType: ExerciseType get() = metrics.exerciseType
    val repCount: Int get() = metrics.repCount
    val validHoldMs: Long get() = metrics.validHoldMs
}
