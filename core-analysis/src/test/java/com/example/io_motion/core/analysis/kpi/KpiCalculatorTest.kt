package com.example.io_motion.core.analysis.kpi

import com.example.io_motion.core.analysis.model.RepMetrics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KpiCalculatorTest {

    // ──────────────────────────────────────────────
    // tempo
    // ──────────────────────────────────────────────

    @Test
    fun `tempo is zero for zero reps`() {
        assertEquals(0.0, KpiCalculator.tempo(0, 60_000L), 1e-9)
    }

    @Test
    fun `tempo is zero for zero duration`() {
        assertEquals(0.0, KpiCalculator.tempo(5, 0L), 1e-9)
    }

    @Test
    fun `tempo computes reps per minute correctly`() {
        // 10 reps in 60 seconds = 10 rpm
        assertEquals(10.0, KpiCalculator.tempo(10, 60_000L), 1e-9)
    }

    @Test
    fun `tempo handles sub-minute duration`() {
        // 5 reps in 30 seconds = 10 rpm
        assertEquals(10.0, KpiCalculator.tempo(5, 30_000L), 1e-9)
    }

    // ──────────────────────────────────────────────
    // rhythmConsistency
    // ──────────────────────────────────────────────

    @Test
    fun `rhythm is 100 with fewer than 2 rep timestamps`() {
        assertEquals(100, KpiCalculator.rhythmConsistency(emptyList()))
        assertEquals(100, KpiCalculator.rhythmConsistency(listOf(1000L)))
    }

    @Test
    fun `perfectly even intervals give 100 consistency`() {
        // 5 reps, each 2000ms apart
        val times = (0L..4L).map { it * 2_000L }
        assertEquals(100, KpiCalculator.rhythmConsistency(times))
    }

    @Test
    fun `highly variable intervals give low consistency`() {
        // Alternating 500ms and 4500ms intervals — very uneven
        val times = listOf(0L, 500L, 5_000L, 5_500L, 10_000L)
        val score = KpiCalculator.rhythmConsistency(times)
        assertTrue("Expected low consistency for erratic intervals, got $score", score < 50)
    }

    @Test
    fun `consistency is always in 0-100 range`() {
        val extremes = listOf(0L, 100L, 10_000L, 10_050L, 20_000L)
        val score = KpiCalculator.rhythmConsistency(extremes)
        assertTrue("Score $score out of [0,100]", score in 0..100)
    }

    // ──────────────────────────────────────────────
    // averageRom
    // ──────────────────────────────────────────────

    @Test
    fun `averageRom is zero for empty list`() {
        assertEquals(0.0, KpiCalculator.averageRom(emptyList()), 1e-9)
    }

    @Test
    fun `averageRom computes mean of rep ROMs`() {
        val reps = listOf(
            RepMetrics(1, 1000L, 80.0, 170.0, 80),  // ROM = 90
            RepMetrics(2, 1000L, 90.0, 170.0, 70),  // ROM = 80
        )
        assertEquals(85.0, KpiCalculator.averageRom(reps), 1e-9)
    }

    // ──────────────────────────────────────────────
    // sessionQuality
    // ──────────────────────────────────────────────

    @Test
    fun `sessionQuality is zero for empty list`() {
        assertEquals(0, KpiCalculator.sessionQuality(emptyList()))
    }

    @Test
    fun `sessionQuality averages rep scores`() {
        val reps = listOf(
            RepMetrics(1, 1000L, 80.0, 170.0, 80),
            RepMetrics(2, 1000L, 90.0, 170.0, 60),
        )
        assertEquals(70, KpiCalculator.sessionQuality(reps))
    }

    // ──────────────────────────────────────────────
    // repQualityScore
    // ──────────────────────────────────────────────

    @Test
    fun `perfect rep gives 100 score`() {
        // minAngle = idealDepth, maxAngle = extendThreshold, formScore = 1.0
        val score = KpiCalculator.repQualityScore(
            minAngle = 80.0,
            maxAngle = 160.0,
            flexThreshold = 100.0,
            extendThreshold = 160.0,
            idealDepth = 80.0,
            formScore = 1.0,
        )
        assertEquals(100, score)
    }

    @Test
    fun `only just reaching flex threshold gives partial depth score`() {
        // minAngle = flexThreshold → depth numerator = 0 → depthScore = 0
        val score = KpiCalculator.repQualityScore(
            minAngle = 100.0,
            maxAngle = 160.0,
            flexThreshold = 100.0,
            extendThreshold = 160.0,
            idealDepth = 80.0,
            formScore = 1.0,
        )
        // depthScore = 0, romScore = (160-100)/(160-100) = 1.0, formScore = 1.0
        // raw = 0*0.5 + 1*0.3 + 1*0.2 = 0.5 → 50
        assertEquals(50, score)
    }

    @Test
    fun `score is always in 0 to 100 range`() {
        val score = KpiCalculator.repQualityScore(
            minAngle = 200.0,  // beyond ideal depth
            maxAngle = 200.0,
            flexThreshold = 100.0,
            extendThreshold = 160.0,
            idealDepth = 80.0,
            formScore = -5.0,
        )
        assertTrue("Score $score out of [0,100]", score in 0..100)
    }

    // ──────────────────────────────────────────────
    // symmetryScore
    // ──────────────────────────────────────────────

    @Test
    fun `perfect symmetry gives 1_0`() {
        assertEquals(1.0, KpiCalculator.symmetryScore(90.0, 90.0, 15.0), 1e-9)
    }

    @Test
    fun `deviation at maxDeviation gives 0_0`() {
        assertEquals(0.0, KpiCalculator.symmetryScore(90.0, 105.0, 15.0), 1e-9)
    }

    @Test
    fun `deviation beyond max is clamped to 0`() {
        assertEquals(0.0, KpiCalculator.symmetryScore(90.0, 120.0, 15.0), 1e-9)
    }

    // ──────────────────────────────────────────────
    // bodyLineScore
    // ──────────────────────────────────────────────

    @Test
    fun `perfect body line gives 1_0`() {
        assertEquals(1.0, KpiCalculator.bodyLineScore(180.0, 25.0), 1e-9)
    }

    @Test
    fun `deviation at tolerance gives 0_0`() {
        assertEquals(0.0, KpiCalculator.bodyLineScore(155.0, 25.0), 1e-9)
    }

    @Test
    fun `deviation beyond tolerance is clamped to 0`() {
        assertEquals(0.0, KpiCalculator.bodyLineScore(140.0, 25.0), 1e-9)
    }
}
