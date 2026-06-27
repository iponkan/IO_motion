package com.example.io_motion.core.analysis.model

import com.example.io_motion.core.common.models.ExerciseType

/**
 * Aggregate metrics computed at the end of one analysis session.
 *
 * For rep-based exercises (squat, sit-up, push-up) the [reps] list is populated.
 * For plank the [reps] list is empty and [validHoldMs] / [avgBodyLineAngle] carry the result.
 *
 * See docs/KPI.md for the full definition of every metric.
 *
 * @param exerciseType Which exercise was performed.
 * @param totalDurationMs Wall-clock length of the session in milliseconds.
 * @param repCount Valid rep count (reps that passed all quality gates).
 * @param rejectedRepCount Attempted reps rejected for insufficient ROM or form.
 * @param tempoRpm Valid reps per minute (0 if no reps or zero duration).
 * @param rhythmConsistency 0–100. 100 = perfectly even inter-rep timing; lower = erratic pace.
 * @param avgRomDegrees Session-average range of motion across valid reps.
 * @param sessionQualityScore 0–100. Average of per-rep quality scores (or form score for plank).
 * @param reps Ordered list of [RepMetrics] for each valid rep.
 * @param validHoldMs (Plank) Duration spent with acceptable body alignment, in milliseconds.
 * @param avgBodyLineAngle (Plank) Mean shoulder–hip–ankle angle during valid-hold segments.
 */
data class SessionMetrics(
    val exerciseType: ExerciseType,
    val totalDurationMs: Long,
    val repCount: Int,
    val rejectedRepCount: Int,
    val tempoRpm: Double,
    val rhythmConsistency: Int,
    val avgRomDegrees: Double,
    val sessionQualityScore: Int,
    val reps: List<RepMetrics>,
    val validHoldMs: Long = 0L,
    val avgBodyLineAngle: Double = 0.0,
)
