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
import kotlin.math.abs

/**
 * Rep-counting analyzer for the squat exercise.
 *
 * Primary angle: knee flexion = hip(23/24) – knee(25/26) – ankle(27/28) using 3D world landmarks.
 * A rep is counted on return to extension after reaching [RepAnalyzerConfig.flexThreshold].
 *
 * Quality scoring per rep (see docs/KPI.md):
 *   - Depth (50 pts): how far below the flex threshold the min knee angle reached.
 *   - ROM (30 pts): total range-of-motion versus the hysteresis band.
 *   - Symmetry (20 pts): left–right knee angle difference at deepest point.
 */
class SquatAnalyzer(config: RepAnalyzerConfig = RepAnalyzerConfig.squat) : RepBasedAnalyzer(config) {

    private var minLeftKnee = Double.MAX_VALUE
    private var minRightKnee = Double.MAX_VALUE

    override fun onEnterFlex(frame: PoseFrame) {
        minLeftKnee = Double.MAX_VALUE
        minRightKnee = Double.MAX_VALUE
    }

    override fun extractAngle(frame: PoseFrame): Double? {
        val left = kneeAngle(frame, PoseLandmarkIndex.LEFT_HIP, PoseLandmarkIndex.LEFT_KNEE, PoseLandmarkIndex.LEFT_ANKLE)
        val right = kneeAngle(frame, PoseLandmarkIndex.RIGHT_HIP, PoseLandmarkIndex.RIGHT_KNEE, PoseLandmarkIndex.RIGHT_ANKLE)

        left?.let { if (it < minLeftKnee) minLeftKnee = it }
        right?.let { if (it < minRightKnee) minRightKnee = it }

        return average(left, right)
    }

    override fun buildRepMetrics(
        minAngle: Double,
        maxAngle: Double,
        durationMs: Long,
        repNumber: Int,
        frame: PoseFrame,
    ): RepMetrics {
        val leftMin = minLeftKnee.takeIf { it < Double.MAX_VALUE } ?: minAngle
        val rightMin = minRightKnee.takeIf { it < Double.MAX_VALUE } ?: minAngle

        val symScore = KpiCalculator.symmetryScore(leftMin, rightMin, config.maxSymmetryDeviation)
        val quality = KpiCalculator.repQualityScore(
            minAngle = minAngle,
            maxAngle = maxAngle,
            flexThreshold = config.flexThreshold,
            extendThreshold = config.extendThreshold,
            idealDepth = config.idealDepth,
            formScore = symScore,
        )

        val alerts = buildList {
            if (minAngle > config.flexThreshold + 5) add(FormAlert.GO_DEEPER)
            if (abs(leftMin - rightMin) > config.maxSymmetryDeviation) add(FormAlert.UNEVEN_SIDES)
        }

        minLeftKnee = Double.MAX_VALUE
        minRightKnee = Double.MAX_VALUE

        return RepMetrics(repNumber, durationMs, minAngle, maxAngle, quality, alerts)
    }

    private fun kneeAngle(frame: PoseFrame, hipIdx: Int, kneeIdx: Int, ankleIdx: Int): Double? {
        val lms = ConfidenceGate.getReliableLandmarks(
            frame, listOf(hipIdx, kneeIdx, ankleIdx), config.visibilityThreshold,
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
