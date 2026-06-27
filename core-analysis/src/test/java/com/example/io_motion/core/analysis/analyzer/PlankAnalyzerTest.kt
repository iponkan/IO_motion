package com.example.io_motion.core.analysis.analyzer

import com.example.io_motion.core.analysis.config.PlankConfig
import com.example.io_motion.core.analysis.filter.OneEuroFilterConfig
import com.example.io_motion.core.analysis.model.AnalyzerState
import com.example.io_motion.core.common.models.Landmark
import com.example.io_motion.core.common.models.PoseLandmarkIndex
import com.example.io_motion.core.common.models.PoseFrame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlankAnalyzerTest {

    // Config with wide-bandwidth filter so angles track inputs with minimal lag
    private val testConfig = PlankConfig(
        bodyLineTolerance = 15.0,
        filterConfig = OneEuroFilterConfig(minCutoff = 500.0, beta = 100.0),
    )

    /**
     * Build a PoseFrame whose world landmarks produce a shoulder–hip–ankle angle ≈ [angleDeg]
     * on both sides. We place shoulder at (0,1,0), hip at (0,0,0), and compute ankle position
     * using the supplementary angle so AngleMath.angleDegrees(shoulder,hip,ankle) = angleDeg.
     *
     *   BA = shoulder - hip = (0, 1, 0)
     *   BC = ankle - hip   = (sin(π - θ), -cos(π - θ), 0)  where θ = angleDeg in radians
     *
     * Both sides get identical landmarks so the average equals the single-side value.
     */
    private fun frameForBodyLine(angleDeg: Double, ts: Long): PoseFrame {
        val theta = Math.toRadians(angleDeg)
        val supplementary = Math.PI - theta
        val ankleX = Math.sin(supplementary).toFloat()
        val ankleY = (-Math.cos(supplementary)).toFloat()

        val defaultLm = Landmark(0f, 0f, 0f, 1f, 1f)
        val shoulder = Landmark(0f, 1f, 0f, 1f, 1f)
        val hip = Landmark(0f, 0f, 0f, 1f, 1f)
        val ankle = Landmark(ankleX, ankleY, 0f, 1f, 1f)

        val world = MutableList(PoseLandmarkIndex.LANDMARK_COUNT) { defaultLm }
        world[PoseLandmarkIndex.LEFT_SHOULDER] = shoulder
        world[PoseLandmarkIndex.RIGHT_SHOULDER] = shoulder
        world[PoseLandmarkIndex.LEFT_HIP] = hip
        world[PoseLandmarkIndex.RIGHT_HIP] = hip
        world[PoseLandmarkIndex.LEFT_ANKLE] = ankle
        world[PoseLandmarkIndex.RIGHT_ANKLE] = ankle

        return PoseFrame(ts, emptyList(), world)
    }

    // ──────────────────────────────────────────────
    // Hold accumulation
    // ──────────────────────────────────────────────

    @Test
    fun `valid hold accumulates time when body line is within tolerance`() {
        val analyzer = PlankAnalyzer(testConfig)
        for (i in 0 until 5) {
            val state = analyzer.update(frameForBodyLine(180.0, i * 100L))
            assertTrue(state is AnalyzerState.HoldTracking)
            assertTrue((state as AnalyzerState.HoldTracking).isFormGood)
        }
        val metrics = analyzer.finish()
        // 5 frames at t=0,100,200,300,400 → 4 intervals of 100ms = 400ms valid
        assertEquals(400L, metrics.validHoldMs)
    }

    @Test
    fun `hold pauses when body line deviation exceeds tolerance`() {
        val analyzer = PlankAnalyzer(testConfig)
        // Good: t=0, 100, 200
        analyzer.update(frameForBodyLine(180.0, 0L))
        analyzer.update(frameForBodyLine(180.0, 100L))
        analyzer.update(frameForBodyLine(180.0, 200L))
        // Bad frame (160° → 20° deviation > 15° tolerance): t=300
        val bad = analyzer.update(frameForBodyLine(160.0, 300L))
        assertFalse((bad as AnalyzerState.HoldTracking).isFormGood)
        // Resume good form: t=400 — no interval yet since gap resets lastGoodFrameMs
        analyzer.update(frameForBodyLine(180.0, 400L))
        val metrics = analyzer.finish()
        // Only t=0→100 and t=100→200 accumulated = 200ms
        assertEquals(200L, metrics.validHoldMs)
    }

    @Test
    fun `HoldTracking carries correct body line angle`() {
        val analyzer = PlankAnalyzer(testConfig)
        // Skip first sample (filter initialises) and check second
        analyzer.update(frameForBodyLine(175.0, 0L))
        val state = analyzer.update(frameForBodyLine(175.0, 100L)) as AnalyzerState.HoldTracking
        // Filtered angle should be close to 175°
        assertEquals(175.0, state.bodyLineAngle, 3.0)
    }

    // ──────────────────────────────────────────────
    // finish() — session metrics
    // ──────────────────────────────────────────────

    @Test
    fun `finish returns zero reps for plank`() {
        val analyzer = PlankAnalyzer(testConfig)
        analyzer.update(frameForBodyLine(180.0, 0L))
        assertEquals(0, analyzer.finish().repCount)
    }

    @Test
    fun `finish returns exercise type PLANK`() {
        val analyzer = PlankAnalyzer(testConfig)
        analyzer.update(frameForBodyLine(180.0, 0L))
        assertEquals(com.example.io_motion.core.common.models.ExerciseType.PLANK, analyzer.finish().exerciseType)
    }

    @Test
    fun `perfect hold session gives quality score 100`() {
        val analyzer = PlankAnalyzer(testConfig)
        // 11 frames at 100ms intervals → 10 intervals = 1000ms total = 1000ms valid
        for (i in 0..10) analyzer.update(frameForBodyLine(180.0, i * 100L))
        val metrics = analyzer.finish()
        assertEquals(100, metrics.sessionQualityScore)
    }

    @Test
    fun `zero good frames gives quality score 0`() {
        val analyzer = PlankAnalyzer(testConfig)
        for (i in 0..5) analyzer.update(frameForBodyLine(150.0, i * 100L))  // all bad
        val metrics = analyzer.finish()
        assertEquals(0, metrics.sessionQualityScore)
    }

    @Test
    fun `LowConfidence when world landmarks list is empty`() {
        val analyzer = PlankAnalyzer(testConfig)
        val state = analyzer.update(PoseFrame(0L, emptyList(), emptyList()))
        assertTrue(state is AnalyzerState.LowConfidence)
    }

    // ──────────────────────────────────────────────
    // Reset
    // ──────────────────────────────────────────────

    @Test
    fun `reset clears valid hold and allows reuse`() {
        val analyzer = PlankAnalyzer(testConfig)
        for (i in 0 until 5) analyzer.update(frameForBodyLine(180.0, i * 100L))
        analyzer.reset()
        // After reset: 2 frames at 0 and 100ms → 100ms valid hold
        analyzer.update(frameForBodyLine(180.0, 0L))
        analyzer.update(frameForBodyLine(180.0, 100L))
        assertEquals(100L, analyzer.finish().validHoldMs)
    }
}
