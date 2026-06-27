package com.example.io_motion.core.analysis.analyzer

import com.example.io_motion.core.analysis.ExerciseAnalyzer
import com.example.io_motion.core.analysis.config.RepAnalyzerConfig
import com.example.io_motion.core.analysis.filter.OneEuroFilter
import com.example.io_motion.core.analysis.kpi.KpiCalculator
import com.example.io_motion.core.analysis.model.AnalyzerState
import com.example.io_motion.core.analysis.model.FormAlert
import com.example.io_motion.core.analysis.model.RepMetrics
import com.example.io_motion.core.analysis.model.RepPhase
import com.example.io_motion.core.analysis.model.SessionMetrics
import com.example.io_motion.core.common.models.PoseFrame

/**
 * Finite-state machine for repetition-based exercises (squat, sit-up, push-up).
 *
 * ## FSM states
 * ```
 *  AWAITING_EXTENSION ──→ EXTENDED ──→ FLEXED ──→ EXTENDED (rep counted)
 *                          ↑                          │
 *                          └──────────────────────────┘
 * ```
 * - **AWAITING_EXTENSION**: waiting for the person to first reach [RepAnalyzerConfig.extendThreshold].
 *   Prevents counting partial reps from a mid-exercise start.
 * - **EXTENDED**: primary angle ≥ extendThreshold. Transition to FLEXED when angle drops below
 *   [RepAnalyzerConfig.flexThreshold].
 * - **FLEXED**: primary angle ≤ flexThreshold. Rep is counted (or rejected) when the angle
 *   returns to or above extendThreshold.
 *
 * ## Hysteresis
 * `extendThreshold` must be greater than `flexThreshold`. The gap between them is the dead-band:
 * angles in `(flexThreshold, extendThreshold)` do not trigger any state transition. This prevents
 * jitter around a single boundary from producing false rep counts.
 *
 * ## Subclass contract
 * - Override [extractAngle] to pull the primary joint angle from [PoseFrame] (returns `null` on
 *   low confidence → results in [AnalyzerState.LowConfidence] for that frame).
 * - Override [onEnterFlex] to reset per-rep side tracking (called once per EXTENDED→FLEXED transition).
 * - Override [buildRepMetrics] to compute exercise-specific quality scores and form alerts.
 */
abstract class RepBasedAnalyzer(protected val config: RepAnalyzerConfig) : ExerciseAnalyzer {

    private enum class FsmState { AWAITING_EXTENSION, EXTENDED, FLEXED }

    private var fsmState = FsmState.AWAITING_EXTENSION
    private var minAngle = Double.MAX_VALUE
    private var maxAngle = 0.0
    private var repStartMs = 0L
    private var sessionStartMs = -1L
    private var lastTimestampMs = 0L

    private var repCount = 0
    private var rejectedCount = 0
    private val reps = mutableListOf<RepMetrics>()
    private val repCompletionTimesMs = mutableListOf<Long>()

    private val angleFilter = OneEuroFilter(config.filterConfig)

    // ──────────────────────────────────────────────
    // Subclass extension points
    // ──────────────────────────────────────────────

    /**
     * Extract the primary joint angle (degrees) from [frame].
     * Return `null` when key landmarks are below the confidence threshold.
     */
    protected abstract fun extractAngle(frame: PoseFrame): Double?

    /**
     * Called once when transitioning from EXTENDED to FLEXED.
     * Subclasses use this to reset per-rep side-tracking state (e.g., left/right min angles).
     */
    protected open fun onEnterFlex(frame: PoseFrame) {}

    /**
     * Build the [RepMetrics] for the completed rep.
     * Called once when transitioning from FLEXED back to EXTENDED for a rep that passed the ROM gate.
     *
     * @param minAngle Minimum primary angle reached during this rep.
     * @param maxAngle Maximum primary angle reached during this rep.
     * @param durationMs Duration from EXTENDED→FLEXED entry to FLEXED→EXTENDED exit.
     * @param repNumber The 1-based ordinal of this rep within the session.
     * @param frame The pose frame at the moment of rep completion (for final form check).
     */
    protected abstract fun buildRepMetrics(
        minAngle: Double,
        maxAngle: Double,
        durationMs: Long,
        repNumber: Int,
        frame: PoseFrame,
    ): RepMetrics

    // ──────────────────────────────────────────────
    // ExerciseAnalyzer implementation
    // ──────────────────────────────────────────────

    override fun update(frame: PoseFrame): AnalyzerState {
        if (sessionStartMs < 0L) sessionStartMs = frame.timestampMs
        lastTimestampMs = frame.timestampMs

        val rawAngle = extractAngle(frame) ?: return AnalyzerState.LowConfidence

        val ts = frame.timestampMs / 1_000.0  // convert ms → seconds for the filter
        val angle = angleFilter.filter(rawAngle, ts)

        advanceFsm(angle, frame)

        val phase = if (fsmState == FsmState.FLEXED) RepPhase.FLEXED else RepPhase.EXTENDED
        return AnalyzerState.Tracking(
            repCount = repCount,
            rejectedCount = rejectedCount,
            primaryAngle = angle,
            phase = phase,
            alerts = formAlerts(angle, frame),
        )
    }

    override fun finish(): SessionMetrics {
        val totalMs = if (sessionStartMs >= 0L) lastTimestampMs - sessionStartMs else 0L
        return SessionMetrics(
            exerciseType = config.exerciseType,
            totalDurationMs = totalMs,
            repCount = repCount,
            rejectedRepCount = rejectedCount,
            tempoRpm = KpiCalculator.tempo(repCount, totalMs),
            rhythmConsistency = KpiCalculator.rhythmConsistency(repCompletionTimesMs),
            avgRomDegrees = KpiCalculator.averageRom(reps),
            sessionQualityScore = KpiCalculator.sessionQuality(reps),
            reps = reps.toList(),
        )
    }

    override fun reset() {
        fsmState = FsmState.AWAITING_EXTENSION
        minAngle = Double.MAX_VALUE
        maxAngle = 0.0
        repStartMs = 0L
        sessionStartMs = -1L
        lastTimestampMs = 0L
        repCount = 0
        rejectedCount = 0
        reps.clear()
        repCompletionTimesMs.clear()
        angleFilter.reset()
    }

    // ──────────────────────────────────────────────
    // FSM logic
    // ──────────────────────────────────────────────

    private fun advanceFsm(angle: Double, frame: PoseFrame) {
        when (fsmState) {
            FsmState.AWAITING_EXTENSION -> {
                if (angle >= config.extendThreshold) {
                    fsmState = FsmState.EXTENDED
                    maxAngle = angle
                }
            }
            FsmState.EXTENDED -> {
                if (angle > maxAngle) maxAngle = angle
                if (angle <= config.flexThreshold) {
                    fsmState = FsmState.FLEXED
                    minAngle = angle
                    repStartMs = frame.timestampMs
                    onEnterFlex(frame)
                }
            }
            FsmState.FLEXED -> {
                if (angle < minAngle) minAngle = angle
                if (angle >= config.extendThreshold) {
                    fsmState = FsmState.EXTENDED
                    val durationMs = frame.timestampMs - repStartMs
                    val rom = maxAngle - minAngle
                    if (rom >= config.minRom) {
                        val metrics = buildRepMetrics(minAngle, maxAngle, durationMs, repCount + 1, frame)
                        reps.add(metrics)
                        repCompletionTimesMs.add(frame.timestampMs)
                        repCount++
                    } else {
                        rejectedCount++
                    }
                    minAngle = Double.MAX_VALUE
                    maxAngle = angle
                }
            }
        }
    }

    /** Subclasses may override to add exercise-specific form alerts. */
    protected open fun formAlerts(angle: Double, frame: PoseFrame): List<FormAlert> = emptyList()
}
