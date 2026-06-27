package com.example.io_motion.core.analysis.analyzer

import com.example.io_motion.core.analysis.config.RepAnalyzerConfig
import com.example.io_motion.core.analysis.confidence.ConfidenceGate
import com.example.io_motion.core.analysis.kpi.KpiCalculator
import com.example.io_motion.core.analysis.model.FormAlert
import com.example.io_motion.core.analysis.model.RepMetrics
import com.example.io_motion.core.common.math.AngleMath
import com.example.io_motion.core.common.math.Vec3
import com.example.io_motion.core.common.models.PoseLandmarkIndex
import com.example.io_motion.core.common.models.PoseFrame

/**
 * Rep-counting analyzer for the sit-up exercise.
 *
 * Primary angle: hip flexion = shoulder(11/12) – hip(23/24) – knee(25/26) using world landmarks.
 * "EXTENDED" = lying flat (large angle ≥ 140°); "FLEXED" = torso raised (angle ≤ 70°).
 * A rep is counted on return to the lying position after reaching the up threshold.
 *
 * Quality scoring per rep (see docs/KPI.md):
 *   - Depth (50 pts): how far the sit-up angle went below the flex threshold.
 *   - ROM (30 pts): total range from lying to up and back.
 *   - Form (20 pts): fixed at 1.0 (form for sit-up is captured by the angle itself).
 */
class SitUpAnalyzer(config: RepAnalyzerConfig = RepAnalyzerConfig.sitUp) : RepBasedAnalyzer(config) {

    override fun extractAngle(frame: PoseFrame): Double? {
        val left = hipFlexionAngle(
            frame,
            PoseLandmarkIndex.LEFT_SHOULDER,
            PoseLandmarkIndex.LEFT_HIP,
            PoseLandmarkIndex.LEFT_KNEE,
        )
        val right = hipFlexionAngle(
            frame,
            PoseLandmarkIndex.RIGHT_SHOULDER,
            PoseLandmarkIndex.RIGHT_HIP,
            PoseLandmarkIndex.RIGHT_KNEE,
        )
        return average(left, right)
    }

    override fun buildRepMetrics(
        minAngle: Double,
        maxAngle: Double,
        durationMs: Long,
        repNumber: Int,
        frame: PoseFrame,
    ): RepMetrics {
        val quality = KpiCalculator.repQualityScore(
            minAngle = minAngle,
            maxAngle = maxAngle,
            flexThreshold = config.flexThreshold,
            extendThreshold = config.extendThreshold,
            idealDepth = config.idealDepth,
            formScore = 1.0,
        )
        val alerts = buildList {
            if (minAngle > config.flexThreshold + 5) add(FormAlert.GO_DEEPER)
        }
        return RepMetrics(repNumber, durationMs, minAngle, maxAngle, quality, alerts)
    }

    private fun hipFlexionAngle(frame: PoseFrame, shoulderIdx: Int, hipIdx: Int, kneeIdx: Int): Double? {
        val lms = ConfidenceGate.getReliableLandmarks(
            frame, listOf(shoulderIdx, hipIdx, kneeIdx), config.visibilityThreshold,
        ) ?: return null
        return AngleMath.angleDegrees(
            Vec3(lms[0].x, lms[0].y, lms[0].z),
            Vec3(lms[1].x, lms[1].y, lms[1].z),
            Vec3(lms[2].x, lms[2].y, lms[2].z),
        ).takeIf { !it.isNaN() }
    }

    private fun average(a: Double?, b: Double?) = when {
        a != null && b != null -> (a + b) / 2.0
        a != null -> a
        b != null -> b
        else -> null
    }
}
