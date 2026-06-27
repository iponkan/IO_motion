package com.example.io_motion.core.analysis.config

import com.example.io_motion.core.analysis.filter.OneEuroFilterConfig
import com.example.io_motion.core.common.models.ExerciseType

/**
 * Threshold configuration for a [com.example.io_motion.core.analysis.analyzer.RepBasedAnalyzer].
 *
 * All angle values are in degrees and apply to the primary joint angle for the exercise.
 *
 * **Hysteresis design:** [flexThreshold] and [extendThreshold] intentionally differ so that
 * jitter near either threshold cannot trigger spurious rep counts. A rep is only counted when
 * the angle crosses [flexThreshold] going *into* flex AND later crosses [extendThreshold] going
 * *back* to extension. The gap between the two thresholds defines the dead-band.
 *
 * @param exerciseType Which exercise this config belongs to.
 * @param flexThreshold Primary angle below which the joint is considered "flexed" (rep bottom).
 * @param extendThreshold Primary angle above which the joint is considered "extended" (rep top).
 *   Must be greater than [flexThreshold].
 * @param minRom Minimum range-of-motion (degrees) required for a rep to count.
 *   Reps with smaller ROM are rejected. Prevents partial-motion false counts.
 * @param idealDepth The angle target for a "perfect" depth (used in the quality score formula).
 *   Deeper than [flexThreshold] gives full depth points; [idealDepth] is the optimum.
 * @param maxSymmetryDeviation Max acceptable left–right angle deviation (degrees) before an
 *   UNEVEN_SIDES alert is raised (applies to squat and sit-up where both sides are visible).
 * @param visibilityThreshold MediaPipe landmark visibility score below which a landmark is
 *   considered unreliable and measurement is suppressed for that frame.
 * @param filterConfig One-Euro filter settings applied to the primary angle signal.
 */
data class RepAnalyzerConfig(
    val exerciseType: ExerciseType,
    val flexThreshold: Double,
    val extendThreshold: Double,
    val minRom: Double,
    val idealDepth: Double,
    val maxSymmetryDeviation: Double = 15.0,
    val visibilityThreshold: Float = 0.5f,
    val filterConfig: OneEuroFilterConfig = OneEuroFilterConfig(),
) {
    init {
        require(extendThreshold > flexThreshold) {
            "extendThreshold ($extendThreshold) must be greater than flexThreshold ($flexThreshold)"
        }
    }

    companion object {
        /**
         * Squat defaults: knee angle (hip–knee–ankle).
         * Standing ≈ 170–180°. Bottom ≈ ≤ 100°.
         */
        val squat = RepAnalyzerConfig(
            exerciseType = ExerciseType.SQUAT,
            flexThreshold = 100.0,
            extendThreshold = 160.0,
            minRom = 40.0,
            idealDepth = 80.0,
        )

        /**
         * Sit-up defaults: hip-flexion angle (shoulder–hip–knee).
         * Lying ≈ ≥ 150°. Up position ≈ ≤ 70°.
         */
        val sitUp = RepAnalyzerConfig(
            exerciseType = ExerciseType.SIT_UP,
            flexThreshold = 70.0,
            extendThreshold = 140.0,
            minRom = 40.0,
            idealDepth = 55.0,
        )

        /**
         * Push-up defaults: elbow angle (shoulder–elbow–wrist).
         * Arms extended ≈ ≥ 160°. Bottom ≈ ≤ 95°.
         */
        val pushUp = RepAnalyzerConfig(
            exerciseType = ExerciseType.PUSH_UP,
            flexThreshold = 95.0,
            extendThreshold = 150.0,
            minRom = 40.0,
            idealDepth = 80.0,
        )
    }
}
