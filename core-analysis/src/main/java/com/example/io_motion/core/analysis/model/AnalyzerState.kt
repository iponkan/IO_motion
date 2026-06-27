package com.example.io_motion.core.analysis.model

/**
 * Per-frame output from [com.example.io_motion.core.analysis.ExerciseAnalyzer.update].
 * The UI observes this stream to drive the live overlay and counter displays.
 */
sealed class AnalyzerState {

    /**
     * Active rep-based tracking. Emitted each frame while landmarks are reliable.
     *
     * @param repCount Valid reps completed so far.
     * @param rejectedCount Attempted reps that did not meet ROM/quality gates.
     * @param primaryAngle Smoothed primary joint angle in degrees (live readout for the UI).
     * @param phase Current position in the rep cycle (EXTENDED or FLEXED).
     * @param alerts Any active form cues for this frame.
     */
    data class Tracking(
        val repCount: Int,
        val rejectedCount: Int,
        val primaryAngle: Double,
        val phase: RepPhase,
        val alerts: List<FormAlert> = emptyList(),
    ) : AnalyzerState()

    /**
     * Plank-specific frame output.
     *
     * @param validHoldMs Valid hold time accumulated so far in milliseconds.
     * @param bodyLineAngle Shoulder–hip–ankle angle in degrees (ideally ≈ 180°).
     * @param isFormGood True when body alignment is within the configured tolerance.
     * @param alerts Any active form cues.
     */
    data class HoldTracking(
        val validHoldMs: Long,
        val bodyLineAngle: Double,
        val isFormGood: Boolean,
        val alerts: List<FormAlert> = emptyList(),
    ) : AnalyzerState()

    /** Key landmarks are below the confidence threshold — measurement suppressed this frame. */
    object LowConfidence : AnalyzerState()

    /** Waiting for a valid starting position before the first rep can begin. */
    object AwaitingStart : AnalyzerState()
}
