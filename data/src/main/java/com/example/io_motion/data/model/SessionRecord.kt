package com.example.io_motion.data.model

import com.example.io_motion.core.analysis.model.SessionMetrics
import com.example.io_motion.core.common.models.AnalysisMode

/** Public view of a persisted session, combining storage metadata with computed metrics. */
data class SessionRecord(
    val id: Long,
    val recordedAt: Long,
    val analysisMode: AnalysisMode,
    val modelVariant: String,
    val metrics: SessionMetrics,
)
