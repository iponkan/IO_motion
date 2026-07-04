package com.example.io_motion.data.repository

import com.example.io_motion.core.analysis.model.SessionMetrics
import com.example.io_motion.core.common.models.AnalysisMode
import com.example.io_motion.data.model.SessionRecord
import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    /** Live-observable list of all sessions, newest first. */
    val sessions: Flow<List<SessionRecord>>

    /**
     * Persists a completed session and returns its generated database ID.
     *
     * @param metrics Computed metrics from the analysis engine.
     * @param mode Whether this was a live-camera or offline-video session.
     * @param modelVariant Name of the pose model variant used (e.g. "FULL").
     * @param recordedAt Session start epoch millis; defaults to now.
     */
    suspend fun save(
        metrics: SessionMetrics,
        mode: AnalysisMode,
        modelVariant: String,
        recordedAt: Long = System.currentTimeMillis(),
    ): Long

    /** Returns the full [SessionRecord] for [id], or null if not found. */
    suspend fun getById(id: Long): SessionRecord?

    /** Permanently removes the session with [id] (cascades to rep rows). */
    suspend fun delete(id: Long)
}
