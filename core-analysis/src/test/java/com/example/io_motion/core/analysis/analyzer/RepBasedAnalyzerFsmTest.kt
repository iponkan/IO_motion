package com.example.io_motion.core.analysis.analyzer

import com.example.io_motion.core.analysis.config.RepAnalyzerConfig
import com.example.io_motion.core.analysis.filter.OneEuroFilterConfig
import com.example.io_motion.core.analysis.model.AnalyzerState
import com.example.io_motion.core.analysis.model.FormAlert
import com.example.io_motion.core.analysis.model.RepMetrics
import com.example.io_motion.core.analysis.model.RepPhase
import com.example.io_motion.core.common.models.ExerciseType
import com.example.io_motion.core.common.models.PoseFrame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * FSM correctness tests using a [FixedAngleAnalyzer] test double that bypasses pose extraction
 * and injects controlled angle values directly into the FSM.
 *
 * All tests use a near-transparent filter (minCutoff=500, beta=100) so the smoothed angle
 * is effectively equal to the input angle. Threshold values are: flex=100°, extend=160°.
 */
class RepBasedAnalyzerFsmTest {

    // Config with near-transparent filter so injected angles pass through unchanged
    private val testConfig = RepAnalyzerConfig(
        exerciseType = ExerciseType.SQUAT,
        flexThreshold = 100.0,
        extendThreshold = 160.0,
        minRom = 30.0,
        idealDepth = 80.0,
        filterConfig = OneEuroFilterConfig(minCutoff = 500.0, beta = 100.0),
    )

    /** Concrete subclass that returns a configurable angle instead of extracting from landmarks. */
    private inner class FixedAngleAnalyzer(config: RepAnalyzerConfig = testConfig) : RepBasedAnalyzer(config) {
        var angle: Double = 180.0

        override fun extractAngle(frame: PoseFrame): Double = angle

        override fun buildRepMetrics(
            minAngle: Double, maxAngle: Double, durationMs: Long, repNumber: Int, frame: PoseFrame,
        ) = RepMetrics(repNumber, durationMs, minAngle, maxAngle, 85, emptyList())
    }

    private var ts = 0L
    private fun frame(): PoseFrame = PoseFrame(ts.also { ts += 33L }, emptyList(), emptyList())
    private fun resetTs() { ts = 0L }

    /** Feed the analyzer [frames] frames at the given angle. */
    private fun FixedAngleAnalyzer.feed(angleDeg: Double, frames: Int = 5): AnalyzerState {
        angle = angleDeg
        var last: AnalyzerState = AnalyzerState.LowConfidence
        repeat(frames) { last = update(frame()) }
        return last
    }

    // ──────────────────────────────────────────────
    // Rep counting
    // ──────────────────────────────────────────────

    @Test
    fun `clean rep is counted`() {
        resetTs()
        val analyzer = FixedAngleAnalyzer()
        analyzer.feed(180.0)   // establish extended
        analyzer.feed(80.0)    // go to flexed
        val state = analyzer.feed(180.0)  // return to extended — rep counted
        assertTrue(state is AnalyzerState.Tracking)
        assertEquals(1, (state as AnalyzerState.Tracking).repCount)
    }

    @Test
    fun `two clean reps are counted`() {
        resetTs()
        val analyzer = FixedAngleAnalyzer()
        analyzer.feed(180.0)
        analyzer.feed(80.0)
        analyzer.feed(180.0)  // rep 1
        analyzer.feed(80.0)
        val state = analyzer.feed(180.0)  // rep 2
        assertEquals(2, (state as AnalyzerState.Tracking).repCount)
    }

    @Test
    fun `phase is FLEXED when below flex threshold`() {
        resetTs()
        val analyzer = FixedAngleAnalyzer()
        analyzer.feed(180.0)
        val state = analyzer.feed(80.0)
        assertEquals(RepPhase.FLEXED, (state as AnalyzerState.Tracking).phase)
    }

    @Test
    fun `phase is EXTENDED when above extend threshold`() {
        resetTs()
        val analyzer = FixedAngleAnalyzer()
        val state = analyzer.feed(180.0)
        assertEquals(RepPhase.EXTENDED, (state as AnalyzerState.Tracking).phase)
    }

    // ──────────────────────────────────────────────
    // Hysteresis — no double counting
    // ──────────────────────────────────────────────

    @Test
    fun `oscillation between flex and extend thresholds does not count extra reps`() {
        // Angle goes to FLEXED then oscillates in the dead-band (100°–160°) without
        // reaching 160° — no rep should be counted yet.
        resetTs()
        val analyzer = FixedAngleAnalyzer()
        analyzer.feed(180.0)     // extended
        analyzer.feed(80.0)      // flexed
        // Oscillate in dead-band
        repeat(5) {
            analyzer.feed(120.0)
            analyzer.feed(110.0)
        }
        val state = analyzer.feed(120.0)
        assertEquals(0, (state as AnalyzerState.Tracking).repCount)
    }

    @Test
    fun `oscillation around flex threshold before reaching extend does not double count`() {
        // Angle bounces around 100° — hysteresis must not trigger multiple FLEXED entries
        resetTs()
        val analyzer = FixedAngleAnalyzer()
        analyzer.feed(180.0)
        // Jitter: 95, 105, 95, 105 → only one FLEXED entry, minimum tracked correctly
        repeat(4) { i ->
            analyzer.feed(if (i % 2 == 0) 95.0 else 105.0)
        }
        analyzer.feed(180.0)  // return to extended — exactly 1 rep
        val state = analyzer.feed(180.0)
        assertEquals(1, (state as AnalyzerState.Tracking).repCount)
    }

    // ──────────────────────────────────────────────
    // Partial rep rejection
    // ──────────────────────────────────────────────

    @Test
    fun `angle that never reaches flex threshold is not counted`() {
        // Only goes to 120° — above flex threshold (100°), so never enters FLEXED state
        resetTs()
        val analyzer = FixedAngleAnalyzer()
        analyzer.feed(180.0)
        analyzer.feed(120.0)   // stays in dead-band, never enters FLEXED
        val state = analyzer.feed(180.0)
        assertEquals(0, (state as AnalyzerState.Tracking).repCount)
        assertEquals(0, (state as AnalyzerState.Tracking).rejectedCount)
    }

    @Test
    fun `rep with insufficient ROM is rejected`() {
        // Goes to FLEXED (80°) from only 120° (not from 160°) — ROM = 120-80 = 40 ≥ minRom(30)
        // So this should actually pass. Let's use a tighter case: go from 135° to 100°, ROM = 35 ≥ 30 → counts
        // For a true rejection: must reach extendThreshold first, then only dip marginally into FLEXED
        // Actually the minRom check is: maxAngle - minAngle >= minRom
        // If we start at extendThreshold (160) and only go to 140 (above flexThreshold 100) → never enters FLEXED
        // To trigger rejection: need to reach FLEXED but with very small overall ROM
        // Since we must cross 100° to enter FLEXED and we need maxAngle < 130 to fail minRom=30...
        // Let's use a custom config with higher minRom
        resetTs()
        val strictConfig = testConfig.copy(minRom = 80.0)  // require 80° ROM
        val analyzer = FixedAngleAnalyzer(strictConfig)
        analyzer.feed(180.0)   // extended: maxAngle = 180
        analyzer.feed(110.0)   // in dead-band: maxAngle still 180
        // Now quickly dip to 95° (below flex threshold) and return — ROM = 180-95 = 85 ≥ 80 → still passes
        // For rejection: need ROM < 80. So we need minAngle > 100° after flex entry...
        // Actually the filter might help here. Let's use a case where angle reaches 80 but with small maxAngle
        // Simplest: reset and go directly from 130° to 80° — ROM = 130-80 = 50 < 80 → rejected
        analyzer.reset()
        resetTs()
        // Must first establish extended (≥160°)
        analyzer.feed(180.0)   // maxAngle = 180, enters EXTENDED
        analyzer.feed(80.0)    // enters FLEXED, minAngle = 80, ROM = 180-80 = 100 ≥ 80 → still counts
        // So with minRom=80 this still passes. Let's use minRom=110
        val veryStrictConfig = testConfig.copy(minRom = 110.0)
        val analyzer2 = FixedAngleAnalyzer(veryStrictConfig)
        resetTs()
        analyzer2.feed(165.0)  // established extended, maxAngle ≈ 165
        analyzer2.feed(80.0)   // enters flexed, ROM = 165-80 = 85 < 110 → rejected
        val state = analyzer2.feed(165.0)  // returns to extended
        assertEquals(0, (state as AnalyzerState.Tracking).repCount)
        assertEquals(1, (state as AnalyzerState.Tracking).rejectedCount)
    }

    @Test
    fun `started mid-exercise awaits first extension before counting`() {
        // Start with angle in flexed zone — must not count until we first see EXTENDED
        resetTs()
        val analyzer = FixedAngleAnalyzer()
        analyzer.feed(80.0)   // start in flexed — AWAITING_EXTENSION, no state change
        analyzer.feed(180.0)  // now EXTENDED
        analyzer.feed(80.0)   // FLEXED
        val state = analyzer.feed(180.0)  // EXTENDED again → 1 rep
        assertEquals(1, (state as AnalyzerState.Tracking).repCount)
    }

    // ──────────────────────────────────────────────
    // min/max angle tracking
    // ──────────────────────────────────────────────

    @Test
    fun `min angle in rep is captured correctly`() {
        resetTs()
        val analyzer = FixedAngleAnalyzer()
        analyzer.feed(180.0)
        analyzer.angle = 90.0; analyzer.update(frame())
        analyzer.angle = 75.0; analyzer.update(frame())  // deepest
        analyzer.angle = 85.0; analyzer.update(frame())
        analyzer.feed(180.0)

        val metrics = analyzer.finish()
        assertEquals(1, metrics.repCount)
        assertEquals(75.0, metrics.reps[0].minAngle, 2.0)  // 2° tolerance for filter
    }

    // ──────────────────────────────────────────────
    // Low confidence
    // ──────────────────────────────────────────────

    @Test
    fun `LowConfidence state when extractAngle returns null`() {
        resetTs()
        val analyzer = object : RepBasedAnalyzer(testConfig) {
            override fun extractAngle(frame: PoseFrame): Double? = null
            override fun buildRepMetrics(minAngle: Double, maxAngle: Double, durationMs: Long, repNumber: Int, frame: PoseFrame) =
                RepMetrics(repNumber, durationMs, minAngle, maxAngle, 100, emptyList())
        }
        val state = analyzer.update(frame())
        assertTrue(state is AnalyzerState.LowConfidence)
    }

    // ──────────────────────────────────────────────
    // Reset
    // ──────────────────────────────────────────────

    @Test
    fun `reset clears rep count and allows reuse`() {
        resetTs()
        val analyzer = FixedAngleAnalyzer()
        analyzer.feed(180.0); analyzer.feed(80.0); analyzer.feed(180.0)
        assertEquals(1, analyzer.finish().repCount)

        analyzer.reset()
        resetTs()
        val state = analyzer.feed(180.0)
        assertEquals(0, (state as AnalyzerState.Tracking).repCount)
    }

    // ──────────────────────────────────────────────
    // finish() — session metrics
    // ──────────────────────────────────────────────

    @Test
    fun `finish with no reps returns repCount zero`() {
        resetTs()
        val analyzer = FixedAngleAnalyzer()
        analyzer.feed(180.0)
        val metrics = analyzer.finish()
        assertEquals(0, metrics.repCount)
        assertEquals(0.0, metrics.tempoRpm, 1e-9)
    }

    @Test
    fun `finish returns correct rep list size`() {
        resetTs()
        val analyzer = FixedAngleAnalyzer()
        analyzer.feed(180.0); analyzer.feed(80.0); analyzer.feed(180.0)
        analyzer.feed(80.0); analyzer.feed(180.0)
        val metrics = analyzer.finish()
        assertEquals(2, metrics.repCount)
        assertEquals(2, metrics.reps.size)
    }

    @Test
    fun `rep numbers are sequential`() {
        resetTs()
        val analyzer = FixedAngleAnalyzer()
        analyzer.feed(180.0); analyzer.feed(80.0); analyzer.feed(180.0)
        analyzer.feed(80.0); analyzer.feed(180.0)
        val metrics = analyzer.finish()
        assertEquals(1, metrics.reps[0].repNumber)
        assertEquals(2, metrics.reps[1].repNumber)
    }

    @Test
    fun `ExerciseAnalyzerFactory creates correct analyzer type`() {
        val factory = com.example.io_motion.core.analysis.ExerciseAnalyzerFactory
        assertTrue(factory.create(ExerciseType.SQUAT) is SquatAnalyzer)
        assertTrue(factory.create(ExerciseType.SIT_UP) is SitUpAnalyzer)
        assertTrue(factory.create(ExerciseType.PUSH_UP) is PushUpAnalyzer)
        assertTrue(factory.create(ExerciseType.PLANK) is PlankAnalyzer)
    }
}
