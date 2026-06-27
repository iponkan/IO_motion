package com.example.io_motion.core.analysis.analyzer

import com.example.io_motion.core.analysis.ExerciseAnalyzer
import com.example.io_motion.core.analysis.config.PlankConfig
import com.example.io_motion.core.analysis.confidence.ConfidenceGate
import com.example.io_motion.core.analysis.filter.OneEuroFilter
import com.example.io_motion.core.analysis.kpi.KpiCalculator
import com.example.io_motion.core.analysis.model.AnalyzerState
import com.example.io_motion.core.analysis.model.FormAlert
import com.example.io_motion.core.analysis.model.SessionMetrics
import com.example.io_motion.core.common.math.AngleMath
import com.example.io_motion.core.common.math.Vec3
import com.example.io_motion.core.common.models.ExerciseType
import com.example.io_motion.core.common.models.PoseLandmarkIndex
import com.example.io_motion.core.common.models.PoseFrame
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Hold-based analyzer for the plank exercise.
 *
 * Tracks the **duration of correct form** rather than rep count. Form is correct while
 * the shoulder–hip–ankle body-line angle is within [PlankConfig.bodyLineTolerance] of 180°.
 * Accumulation pauses automatically when form breaks and resumes when it is restored.
 *
 * Quality score = percentage of total duration spent in correct-form holds, scaled to 0–100.
 */
class PlankAnalyzer(private val config: PlankConfig = PlankConfig()) : ExerciseAnalyzer {

    private var sessionStartMs = -1L
    private var lastTimestampMs = 0L
    private var validHoldMs = 0L
    private var lastGoodFrameMs = -1L
    private var bodyLineSum = 0.0
    private var bodyLineCount = 0

    private val angleFilter = OneEuroFilter(config.filterConfig)

    override fun update(frame: PoseFrame): AnalyzerState {
        if (sessionStartMs < 0L) sessionStartMs = frame.timestampMs
        lastTimestampMs = frame.timestampMs

        val rawAngle = bodyLineAngle(frame) ?: run {
            lastGoodFrameMs = -1L
            return AnalyzerState.LowConfidence
        }

        val ts = frame.timestampMs / 1_000.0
        val angle = angleFilter.filter(rawAngle, ts)

        val deviation = abs(angle - 180.0)
        val isGood = deviation <= config.bodyLineTolerance

        if (isGood) {
            if (lastGoodFrameMs >= 0L) {
                validHoldMs += frame.timestampMs - lastGoodFrameMs
            }
            lastGoodFrameMs = frame.timestampMs
            bodyLineSum += angle
            bodyLineCount++
        } else {
            lastGoodFrameMs = -1L
        }

        val alerts = bodyLineAlerts(angle)
        return AnalyzerState.HoldTracking(
            validHoldMs = validHoldMs,
            bodyLineAngle = angle,
            isFormGood = isGood,
            alerts = alerts,
        )
    }

    override fun finish(): SessionMetrics {
        val totalMs = if (sessionStartMs >= 0L) lastTimestampMs - sessionStartMs else 0L
        val avgBodyLine = if (bodyLineCount > 0) bodyLineSum / bodyLineCount else 180.0
        val formRatio = if (totalMs > 0L) validHoldMs.toDouble() / totalMs else 0.0
        val qualityScore = (formRatio * 100.0).roundToInt().coerceIn(0, 100)
        return SessionMetrics(
            exerciseType = ExerciseType.PLANK,
            totalDurationMs = totalMs,
            repCount = 0,
            rejectedRepCount = 0,
            tempoRpm = 0.0,
            rhythmConsistency = 100,
            avgRomDegrees = 0.0,
            sessionQualityScore = qualityScore,
            reps = emptyList(),
            validHoldMs = validHoldMs,
            avgBodyLineAngle = avgBodyLine,
        )
    }

    override fun reset() {
        sessionStartMs = -1L
        lastTimestampMs = 0L
        validHoldMs = 0L
        lastGoodFrameMs = -1L
        bodyLineSum = 0.0
        bodyLineCount = 0
        angleFilter.reset()
    }

    private fun bodyLineAngle(frame: PoseFrame): Double? {
        val left = sideBodyLine(
            frame,
            PoseLandmarkIndex.LEFT_SHOULDER,
            PoseLandmarkIndex.LEFT_HIP,
            PoseLandmarkIndex.LEFT_ANKLE,
        )
        val right = sideBodyLine(
            frame,
            PoseLandmarkIndex.RIGHT_SHOULDER,
            PoseLandmarkIndex.RIGHT_HIP,
            PoseLandmarkIndex.RIGHT_ANKLE,
        )
        return when {
            left != null && right != null -> (left + right) / 2.0
            left != null -> left
            right != null -> right
            else -> null
        }
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

    private fun bodyLineAlerts(angle: Double): List<FormAlert> {
        val deviation = angle - 180.0
        return buildList {
            when {
                deviation < -config.bodyLineTolerance -> add(FormAlert.SAGGING_HIPS)
                deviation > config.bodyLineTolerance -> add(FormAlert.PIKING_HIPS)
                abs(deviation) > config.bodyLineTolerance * 0.5 -> add(FormAlert.STRAIGHTEN_BODY_LINE)
            }
        }
    }
}
