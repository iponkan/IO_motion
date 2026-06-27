package com.example.io_motion.core.analysis

import com.example.io_motion.core.analysis.model.AnalyzerState
import com.example.io_motion.core.analysis.model.SessionMetrics
import com.example.io_motion.core.common.models.PoseFrame

/**
 * Contract for all per-exercise analysis engines.
 *
 * Usage within a session:
 * 1. Call [update] for every incoming [PoseFrame]. The returned [AnalyzerState] drives the
 *    live UI (rep counter, angle display, form cues).
 * 2. When the session ends, call [finish] once to obtain the full [SessionMetrics].
 * 3. Call [reset] before reusing the same instance for a new session.
 *
 * Implementations live in `:core-analysis` (Android-free). Both live and offline modes
 * feed [PoseFrame] objects through the same analyzer, producing identical metric definitions.
 */
interface ExerciseAnalyzer {

    /**
     * Process one pose frame and return the current analyzer state.
     * Called at camera / video frame rate (typically 30–60 fps).
     */
    fun update(frame: PoseFrame): AnalyzerState

    /**
     * Compute and return the complete session metrics.
     * Must be called after all frames have been fed through [update].
     * Calling [update] after [finish] has undefined behavior.
     */
    fun finish(): SessionMetrics

    /** Reset all internal state so the analyzer can be reused for a new session. */
    fun reset()
}
