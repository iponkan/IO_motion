package com.example.io_motion.data.repository

import com.example.io_motion.core.analysis.model.SessionMetrics
import com.example.io_motion.core.common.models.AnalysisMode
import com.example.io_motion.data.model.SessionRecord
import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    /** Live-observable list of all sessions, newest first. */
    val sessions: Flow<List<SessionRecord>>

    /**
     * Schedules a completed session to be persisted on an application-scoped coroutine.
     *
     * This is intentionally not a `suspend` function awaited by the caller: it must keep
     * writing even if the calling ViewModel/screen is torn down immediately after this call
     * returns (e.g. the user backs out right after stopping a session).
     *
     * @param metrics Computed metrics from the analysis engine.
     * @param mode Whether this was a live-camera or offline-video session.
     * @param modelVariant Name of the pose model variant used (e.g. "FULL").
     * @param recordedAt Session start epoch millis; defaults to now.
     */
    fun save(
        metrics: SessionMetrics,
        mode: AnalysisMode,
        modelVariant: String,
        recordedAt: Long = System.currentTimeMillis(),
    )

    /** Returns the full [SessionRecord] for [id], or null if not found. */
    suspend fun getById(id: Long): SessionRecord?

    /** Permanently removes the session with [id] (cascades to rep rows). */
    suspend fun delete(id: Long)
}
