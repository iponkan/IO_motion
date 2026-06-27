package com.example.io_motion.core.analysis.filter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OneEuroFilterTest {

    private val dt = 1.0 / 30.0  // 30 fps

    // ──────────────────────────────────────────────
    // Initialisation
    // ──────────────────────────────────────────────

    @Test
    fun `first sample passes through unchanged`() {
        val f = OneEuroFilter()
        assertEquals(5.0, f.filter(5.0, 0.0), 1e-12)
    }

    @Test
    fun `second sample at same timestamp is returned without NaN`() {
        val f = OneEuroFilter()
        f.filter(0.0, 0.0)
        val result = f.filter(1.0, 0.0)  // dt clamped to 1e-6
        assertFalse("Must not be NaN on non-advancing timestamp", result.isNaN())
    }

    // ──────────────────────────────────────────────
    // Steady-state convergence
    // ──────────────────────────────────────────────

    @Test
    fun `constant signal converges to the constant`() {
        val f = OneEuroFilter(minCutoff = 1.0, beta = 0.0)
        val value = 3.14
        var result = f.filter(value, 0.0)
        for (i in 1..300) {
            result = f.filter(value, i * dt)
        }
        assertEquals(value, result, 0.001)
    }

    @Test
    fun `output stays bounded and finite on oscillating input`() {
        val f = OneEuroFilter()
        for (i in 0 until 200) {
            val x = if (i % 2 == 0) 0.0 else 1.0
            val out = f.filter(x, i * dt)
            assertFalse("NaN at step $i", out.isNaN())
            assertTrue("Out of [-1, 2] range at step $i", out in -1.0..2.0)
        }
    }

    // ──────────────────────────────────────────────
    // No NaN guarantee
    // ──────────────────────────────────────────────

    @Test
    fun `no NaN for a ramp signal over many frames`() {
        val f = OneEuroFilter()
        for (i in 0 until 500) {
            val out = f.filter(i.toDouble(), i * dt)
            assertFalse("NaN at step $i", out.isNaN())
        }
    }

    @Test
    fun `no NaN on large magnitude values`() {
        val f = OneEuroFilter()
        val out1 = f.filter(1e6, 0.0)
        val out2 = f.filter(-1e6, dt)
        assertFalse(out1.isNaN())
        assertFalse(out2.isNaN())
    }

    // ──────────────────────────────────────────────
    // Reset behaviour
    // ──────────────────────────────────────────────

    @Test
    fun `after reset first sample passes through unchanged again`() {
        val f = OneEuroFilter()
        f.filter(10.0, 0.0)
        f.filter(10.0, dt)
        f.reset()
        assertEquals(20.0, f.filter(20.0, 0.0), 1e-12)
    }

    @Test
    fun `reset clears derivative memory`() {
        val f = OneEuroFilter(minCutoff = 1.0, beta = 1.0)
        // Drive the derivative estimator high by a large step
        f.filter(0.0, 0.0)
        f.filter(100.0, dt)
        f.reset()
        // After reset the first sample returns as-is; second sample should be smooth again
        f.filter(0.0, 0.0)
        val smoothed = f.filter(1.0, dt)
        // Should be smoothed (not near 100 due to carried-over derivative state)
        assertTrue("Derivative state should be cleared after reset", smoothed < 10.0)
    }

    // ──────────────────────────────────────────────
    // Beta (speed coefficient) behaviour
    // ──────────────────────────────────────────────

    @Test
    fun `higher beta reduces lag on step response`() {
        val slowFilter = OneEuroFilter(minCutoff = 1.0, beta = 0.0, dCutoff = 1.0)
        val fastFilter = OneEuroFilter(minCutoff = 1.0, beta = 10.0, dCutoff = 1.0)

        // Step from 0 → 1 at t=0; run 60 frames
        slowFilter.filter(0.0, 0.0)
        fastFilter.filter(0.0, 0.0)
        var slow = 0.0
        var fast = 0.0
        for (i in 1..60) {
            slow = slowFilter.filter(1.0, i * dt)
            fast = fastFilter.filter(1.0, i * dt)
        }
        assertTrue("High-beta filter should track step more closely", fast > slow)
    }

    // ──────────────────────────────────────────────
    // Config constructor
    // ──────────────────────────────────────────────

    @Test
    fun `config constructor forwards parameters correctly`() {
        val config = OneEuroFilterConfig(minCutoff = 2.0, beta = 0.5, dCutoff = 0.5)
        val f = OneEuroFilter(config)
        assertEquals(2.0, f.minCutoff, 1e-12)
        assertEquals(0.5, f.beta, 1e-12)
        assertEquals(0.5, f.dCutoff, 1e-12)
    }

    // ──────────────────────────────────────────────
    // 3D filter wrapper
    // ──────────────────────────────────────────────

    @Test
    fun `OneEuroFilter3D first sample passes through unchanged`() {
        val f = OneEuroFilter3D()
        val v = com.example.io_motion.core.common.math.Vec3(1.0, 2.0, 3.0)
        val out = f.filter(v, 0.0)
        assertEquals(1.0, out.x, 1e-12)
        assertEquals(2.0, out.y, 1e-12)
        assertEquals(3.0, out.z, 1e-12)
    }

    @Test
    fun `OneEuroFilter3D reset allows reuse`() {
        val f = OneEuroFilter3D()
        val v = com.example.io_motion.core.common.math.Vec3(5.0, 5.0, 5.0)
        f.filter(v, 0.0)
        f.filter(v, dt)
        f.reset()
        val out = f.filter(com.example.io_motion.core.common.math.Vec3(9.0, 9.0, 9.0), 0.0)
        assertEquals(9.0, out.x, 1e-12)
        assertEquals(9.0, out.y, 1e-12)
        assertEquals(9.0, out.z, 1e-12)
    }
}
