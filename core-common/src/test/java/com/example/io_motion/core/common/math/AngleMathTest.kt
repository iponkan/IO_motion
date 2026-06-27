package com.example.io_motion.core.common.math

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AngleMathTest {

    // ──────────────────────────────────────────────
    // angleDegrees
    // ──────────────────────────────────────────────

    @Test
    fun `right angle between perpendicular vectors`() {
        val a = Vec3(1.0, 0.0, 0.0)
        val b = Vec3(0.0, 0.0, 0.0)
        val c = Vec3(0.0, 1.0, 0.0)
        assertEquals(90.0, AngleMath.angleDegrees(a, b, c), 1e-9)
    }

    @Test
    fun `straight line gives 180 degrees`() {
        val a = Vec3(-1.0, 0.0, 0.0)
        val b = Vec3(0.0, 0.0, 0.0)
        val c = Vec3(1.0, 0.0, 0.0)
        assertEquals(180.0, AngleMath.angleDegrees(a, b, c), 1e-9)
    }

    @Test
    fun `zero angle when a and c are in the same direction from b`() {
        val a = Vec3(1.0, 0.0, 0.0)
        val b = Vec3(0.0, 0.0, 0.0)
        val c = Vec3(2.0, 0.0, 0.0)
        assertEquals(0.0, AngleMath.angleDegrees(a, b, c), 1e-9)
    }

    @Test
    fun `45 degree angle in XY plane`() {
        val a = Vec3(1.0, 0.0, 0.0)
        val b = Vec3(0.0, 0.0, 0.0)
        val c = Vec3(1.0, 1.0, 0.0)
        assertEquals(45.0, AngleMath.angleDegrees(a, b, c), 1e-9)
    }

    @Test
    fun `60 degree angle using equilateral triangle geometry`() {
        // Equilateral triangle: all angles are 60°
        val a = Vec3(0.0, 0.0, 0.0)
        val b = Vec3(1.0, 0.0, 0.0)
        val c = Vec3(0.5, 0.866025403, 0.0)
        assertEquals(60.0, AngleMath.angleDegrees(a, b, c), 1e-5)
    }

    @Test
    fun `angle is symmetric — swapping a and c gives same result`() {
        val a = Vec3(1.0, 2.0, 0.0)
        val b = Vec3(0.0, 0.0, 0.0)
        val c = Vec3(3.0, -1.0, 0.0)
        assertEquals(
            AngleMath.angleDegrees(a, b, c),
            AngleMath.angleDegrees(c, b, a),
            1e-12,
        )
    }

    @Test
    fun `degenerate case — a equals b returns NaN`() {
        val a = Vec3(0.0, 0.0, 0.0)
        val b = Vec3(0.0, 0.0, 0.0)
        val c = Vec3(1.0, 0.0, 0.0)
        assertTrue("Expected NaN for degenerate input", AngleMath.angleDegrees(a, b, c).isNaN())
    }

    @Test
    fun `degenerate case — c equals b returns NaN`() {
        val a = Vec3(1.0, 0.0, 0.0)
        val b = Vec3(0.0, 0.0, 0.0)
        val c = Vec3(0.0, 0.0, 0.0)
        assertTrue("Expected NaN for degenerate input", AngleMath.angleDegrees(a, b, c).isNaN())
    }

    @Test
    fun `acos clamping prevents NaN from near-parallel vectors with rounding`() {
        // Vectors nearly identical — cos should be just above 1.0 before clamping
        val a = Vec3(1.0, 1e-15, 0.0)
        val b = Vec3(0.0, 0.0, 0.0)
        val c = Vec3(1.0, 0.0, 0.0)
        val angle = AngleMath.angleDegrees(a, b, c)
        assertFalse("Angle must not be NaN", angle.isNaN())
        assertTrue("Angle must be in [0, 180]", angle in 0.0..180.0)
    }

    @Test
    fun `result is always in 0 to 180 range`() {
        val pairs = listOf(
            Triple(Vec3(1.0, 0.0, 0.0), Vec3(0.0, 0.0, 0.0), Vec3(0.0, 1.0, 0.0)),
            Triple(Vec3(1.0, 1.0, 0.0), Vec3(0.0, 0.0, 0.0), Vec3(-1.0, 1.0, 0.0)),
            Triple(Vec3(0.0, 1.0, 1.0), Vec3(1.0, 0.0, 0.0), Vec3(2.0, 1.0, -1.0)),
        )
        for ((a, b, c) in pairs) {
            val angle = AngleMath.angleDegrees(a, b, c)
            assertTrue("Angle $angle must be in [0, 180]", angle in 0.0..180.0)
        }
    }

    // ──────────────────────────────────────────────
    // distance
    // ──────────────────────────────────────────────

    @Test
    fun `distance of 3-4-0 triangle gives 5`() {
        val a = Vec3(0.0, 0.0, 0.0)
        val b = Vec3(3.0, 4.0, 0.0)
        assertEquals(5.0, AngleMath.distance(a, b), 1e-12)
    }

    @Test
    fun `distance is commutative`() {
        val a = Vec3(1.0, 2.0, 3.0)
        val b = Vec3(4.0, 6.0, 8.0)
        assertEquals(AngleMath.distance(a, b), AngleMath.distance(b, a), 1e-12)
    }

    @Test
    fun `distance to self is zero`() {
        val p = Vec3(1.0, 2.0, 3.0)
        assertEquals(0.0, AngleMath.distance(p, p), 1e-12)
    }

    // ──────────────────────────────────────────────
    // midpoint
    // ──────────────────────────────────────────────

    @Test
    fun `midpoint of two points`() {
        val a = Vec3(0.0, 0.0, 0.0)
        val b = Vec3(2.0, 4.0, 6.0)
        val mid = AngleMath.midpoint(a, b)
        assertEquals(1.0, mid.x, 1e-12)
        assertEquals(2.0, mid.y, 1e-12)
        assertEquals(3.0, mid.z, 1e-12)
    }

    @Test
    fun `midpoint is equidistant from both endpoints`() {
        val a = Vec3(1.0, 3.0, -2.0)
        val b = Vec3(5.0, -1.0, 4.0)
        val mid = AngleMath.midpoint(a, b)
        assertEquals(AngleMath.distance(a, mid), AngleMath.distance(b, mid), 1e-10)
    }
}
