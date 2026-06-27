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
 * Rep-counting analyzer for the push-up exercise.
 *
 * Primary angle: elbow flexion = shoulder(11/12) – elbow(13/14) – wrist(15/16) via world landmarks.
 * "EXTENDED" = arms straight (≥ 150°); "FLEXED" = chest near ground (≤ 95°).
 *
 * Additionally validates body-line straightness (shoulder–hip–ankle ≈ 180°) and penalises
 * sagging or piking in the form score and alerts.
 *
 * Body-line tolerance for form scoring: 25°. Beyond this, the body-line score reaches 0.
 *
 * Quality scoring per rep (see docs/KPI.md):
 *   - Depth (50 pts): how far the elbow angle went below the flex threshold.
 *   - ROM (30 pts): total range of elbow motion.
 *   - Body-line (20 pts): shoulder–hip–ankle alignment at rep completion.
 */
class PushUpAnalyzer(config: RepAnalyzerConfig = RepAnalyzerConfig.pushUp) : RepBasedAnalyzer(config) {

    private val bodyLineTolerance = 25.0

    override fun extractAngle(frame: PoseFrame): Double? {
        val left = elbowAngle(
            frame,
            PoseLandmarkIndex.LEFT_SHOULDER,
            PoseLandmarkIndex.LEFT_ELBOW,
            PoseLandmarkIndex.LEFT_WRIST,
        )
        val right = elbowAngle(
            frame,
            PoseLandmarkIndex.RIGHT_SHOULDER,
            PoseLandmarkIndex.RIGHT_ELBOW,
            PoseLandmarkIndex.RIGHT_WRIST,
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
        val bodyLine = bodyLineAngle(frame) ?: 180.0
        val formScore = KpiCalculator.bodyLineScore(bodyLine, bodyLineTolerance)
        val quality = KpiCalculator.repQualityScore(
            minAngle = minAngle,
            maxAngle = maxAngle,
            flexThreshold = config.flexThreshold,
            extendThreshold = config.extendThreshold,
            idealDepth = config.idealDepth,
            formScore = formScore,
        )
        val deviation = bodyLine - 180.0
        val alerts = buildList {
            if (minAngle > config.flexThreshold + 5) add(FormAlert.GO_DEEPER)
            when {
                deviation < -bodyLineTolerance -> add(FormAlert.SAGGING_HIPS)
                deviation > bodyLineTolerance -> add(FormAlert.PIKING_HIPS)
                abs(deviation) > bodyLineTolerance * 0.5 -> add(FormAlert.STRAIGHTEN_BODY_LINE)
            }
        }
        return RepMetrics(repNumber, durationMs, minAngle, maxAngle, quality, alerts)
    }

    override fun formAlerts(angle: Double, frame: PoseFrame): List<FormAlert> {
        val bodyLine = bodyLineAngle(frame) ?: return emptyList()
        val deviation = bodyLine - 180.0
        return buildList {
            when {
                deviation < -bodyLineTolerance -> add(FormAlert.SAGGING_HIPS)
                deviation > bodyLineTolerance -> add(FormAlert.PIKING_HIPS)
            }
        }
    }

    private fun elbowAngle(frame: PoseFrame, shoulderIdx: Int, elbowIdx: Int, wristIdx: Int): Double? {
        val lms = ConfidenceGate.getReliableLandmarks(
            frame, listOf(shoulderIdx, elbowIdx, wristIdx), config.visibilityThreshold,
        ) ?: return null
        return AngleMath.angleDegrees(
            Vec3(lms[0].x, lms[0].y, lms[0].z),
            Vec3(lms[1].x, lms[1].y, lms[1].z),
            Vec3(lms[2].x, lms[2].y, lms[2].z),
        ).takeIf { !it.isNaN() }
    }

    private fun bodyLineAngle(frame: PoseFrame): Double? {
        // Use one visible side; prefer left, fall back to right
        val left = sideBodyLine(frame, PoseLandmarkIndex.LEFT_SHOULDER, PoseLandmarkIndex.LEFT_HIP, PoseLandmarkIndex.LEFT_ANKLE)
        val right = sideBodyLine(frame, PoseLandmarkIndex.RIGHT_SHOULDER, PoseLandmarkIndex.RIGHT_HIP, PoseLandmarkIndex.RIGHT_ANKLE)
        return average(left, right)
    }

    private fun sideBodyLine(frame: PoseFrame, shoulderIdx: Int, hipIdx: Int, ankleIdx: Int): Double? {
        val lms = ConfidenceGate.getReliableLandmarks(
            frame, listOf(shoulderIdx, hipIdx, ankleIdx), config.visibilityThreshold,
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
