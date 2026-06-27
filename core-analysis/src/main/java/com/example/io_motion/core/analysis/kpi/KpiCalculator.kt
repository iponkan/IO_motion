package com.example.io_motion.core.analysis.kpi

import com.example.io_motion.core.analysis.model.RepMetrics
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Pure functions for computing session-level KPIs from raw rep data.
 *
 * All functions are stateless — they operate on already-collected rep lists and timestamps.
 * See docs/KPI.md for the precise definition and formula of every metric.
 */
object KpiCalculator {

    /**
     * Valid reps per minute.
     * Returns 0.0 when [durationMs] ≤ 0 or [repCount] is 0.
     */
    fun tempo(repCount: Int, durationMs: Long): Double {
        if (repCount == 0 || durationMs <= 0L) return 0.0
        return repCount / (durationMs / 60_000.0)
    }

    /**
     * Rhythm consistency score (0–100). 100 = perfectly even inter-rep intervals; lower = erratic.
     *
     * Formula:
     *   intervals  = consecutive differences between rep completion timestamps (ms)
     *   CV         = (stdDev / mean) × 100   [coefficient of variation, percent]
     *   consistency = clamp(100 − CV, 0, 100), rounded to nearest integer
     *
     * Returns 100 when fewer than 2 reps have been completed (no inter-rep interval to measure).
     */
    fun rhythmConsistency(repCompletionTimesMs: List<Long>): Int {
        if (repCompletionTimesMs.size < 2) return 100
        val intervals = repCompletionTimesMs.zipWithNext { a, b -> (b - a).toDouble() }
        val mean = intervals.average()
        if (mean <= 0.0) return 100
        val variance = intervals.sumOf { d -> (d - mean) * (d - mean) } / intervals.size
        val stdDev = sqrt(variance)
        val cv = (stdDev / mean) * 100.0
        return (100.0 - cv).coerceIn(0.0, 100.0).roundToInt()
    }

    /**
     * Session-average range of motion across [reps] in degrees.
     * Returns 0.0 for an empty rep list.
     */
    fun averageRom(reps: List<RepMetrics>): Double {
        if (reps.isEmpty()) return 0.0
        return reps.map { it.rom }.average()
    }

    /**
     * Session quality score (0–100) = arithmetic mean of per-rep quality scores.
     * Returns 0 for an empty rep list.
     */
    fun sessionQuality(reps: List<RepMetrics>): Int {
        if (reps.isEmpty()) return 0
        return reps.map { it.qualityScore }.average().roundToInt()
    }

    /**
     * Per-rep quality score (0–100) for rep-based exercises.
     *
     * Formula (see docs/KPI.md):
     *   depthScore  = clamp((flexThreshold − minAngle) / (flexThreshold − idealDepth), 0, 1)
     *   romScore    = clamp((maxAngle − minAngle) / (extendThreshold − flexThreshold), 0, 1)
     *   formScore   = caller-supplied [0, 1] (symmetry, body-line, or 1.0 if not applicable)
     *
     *   rawScore = depthScore × 0.50 + romScore × 0.30 + formScore × 0.20
     *   qualityScore = round(rawScore × 100), clamped to [0, 100]
     */
    fun repQualityScore(
        minAngle: Double,
        maxAngle: Double,
        flexThreshold: Double,
        extendThreshold: Double,
        idealDepth: Double,
        formScore: Double = 1.0,
    ): Int {
        val depthRange = flexThreshold - idealDepth
        val depthScore = if (depthRange <= 0.0) 1.0 else
            ((flexThreshold - minAngle) / depthRange).coerceIn(0.0, 1.0)

        val threshRange = extendThreshold - flexThreshold
        val romScore = if (threshRange <= 0.0) 1.0 else
            ((maxAngle - minAngle) / threshRange).coerceIn(0.0, 1.0)

        val raw = depthScore * 0.50 + romScore * 0.30 + formScore.coerceIn(0.0, 1.0) * 0.20
        return (raw * 100.0).roundToInt().coerceIn(0, 100)
    }

    /**
     * Symmetry form score (0–1) for exercises with bilateral joint comparison (squat, sit-up).
     *
     * @param leftAngle  Minimum primary angle on the left side during this rep.
     * @param rightAngle Minimum primary angle on the right side during this rep.
     * @param maxDeviation Degrees of left–right difference allowed before score reaches 0.
     */
    fun symmetryScore(leftAngle: Double, rightAngle: Double, maxDeviation: Double): Double {
        if (maxDeviation <= 0.0) return 1.0
        val deviation = kotlin.math.abs(leftAngle - rightAngle)
        return (1.0 - deviation / maxDeviation).coerceIn(0.0, 1.0)
    }

    /**
     * Body-line form score (0–1) for push-up and plank.
     *
     * @param bodyLineAngle Shoulder–hip–ankle angle in degrees (ideally ≈ 180°).
     * @param tolerance Max acceptable deviation from 180° before score reaches 0.
     */
    fun bodyLineScore(bodyLineAngle: Double, tolerance: Double): Double {
        if (tolerance <= 0.0) return 1.0
        val deviation = kotlin.math.abs(bodyLineAngle - 180.0)
        return (1.0 - deviation / tolerance).coerceIn(0.0, 1.0)
    }
}
