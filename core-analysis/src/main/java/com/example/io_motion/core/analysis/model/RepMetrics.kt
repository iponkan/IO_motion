package com.example.io_motion.core.analysis.model

/**
 * Captured measurements for a single valid repetition.
 *
 * @param repNumber 1-based ordinal within the session.
 * @param durationMs Wall-clock duration of this rep (descent + ascent) in milliseconds.
 * @param minAngle Minimum primary joint angle reached during this rep (degrees). Lower = deeper.
 * @param maxAngle Maximum primary joint angle reached during this rep (degrees).
 * @param qualityScore Per-rep quality 0–100. See docs/KPI.md for the exact formula.
 * @param alerts Form alerts observed during this rep.
 */
data class RepMetrics(
    val repNumber: Int,
    val durationMs: Long,
    val minAngle: Double,
    val maxAngle: Double,
    val qualityScore: Int,
    val alerts: List<FormAlert> = emptyList(),
) {
    /** Actual range of motion for this rep in degrees. */
    val rom: Double get() = maxAngle - minAngle
}
