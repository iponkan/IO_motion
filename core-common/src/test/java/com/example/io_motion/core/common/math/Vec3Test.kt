package com.example.io_motion.core.common.math

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

class Vec3Test {

    @Test
    fun `subtraction produces correct components`() {
        val result = Vec3(3.0, 5.0, 7.0) - Vec3(1.0, 2.0, 3.0)
        assertEquals(2.0, result.x, 1e-12)
        assertEquals(3.0, result.y, 1e-12)
        assertEquals(4.0, result.z, 1e-12)
    }

    @Test
    fun `addition produces correct components`() {
        val result = Vec3(1.0, 2.0, 3.0) + Vec3(4.0, 5.0, 6.0)
        assertEquals(5.0, result.x, 1e-12)
        assertEquals(7.0, result.y, 1e-12)
        assertEquals(9.0, result.z, 1e-12)
    }

    @Test
    fun `scalar multiplication scales all components`() {
        val result = Vec3(1.0, 2.0, 3.0) * 2.0
        assertEquals(2.0, result.x, 1e-12)
        assertEquals(4.0, result.y, 1e-12)
        assertEquals(6.0, result.z, 1e-12)
    }

    @Test
    fun `scalar division scales all components`() {
        val result = Vec3(2.0, 4.0, 6.0) / 2.0
        assertEquals(1.0, result.x, 1e-12)
        assertEquals(2.0, result.y, 1e-12)
        assertEquals(3.0, result.z, 1e-12)
    }

    @Test
    fun `dot product of perpendicular vectors is zero`() {
        val a = Vec3(1.0, 0.0, 0.0)
        val b = Vec3(0.0, 1.0, 0.0)
        assertEquals(0.0, a.dot(b), 1e-12)
    }

    @Test
    fun `dot product of parallel vectors equals product of magnitudes`() {
        val a = Vec3(2.0, 0.0, 0.0)
        val b = Vec3(3.0, 0.0, 0.0)
        assertEquals(6.0, a.dot(b), 1e-12)
    }

    @Test
    fun `magnitude of unit axis vector is 1`() {
        assertEquals(1.0, Vec3(1.0, 0.0, 0.0).magnitude, 1e-12)
        assertEquals(1.0, Vec3(0.0, 1.0, 0.0).magnitude, 1e-12)
        assertEquals(1.0, Vec3(0.0, 0.0, 1.0).magnitude, 1e-12)
    }

    @Test
    fun `magnitude of 3-4-0 vector is 5`() {
        assertEquals(5.0, Vec3(3.0, 4.0, 0.0).magnitude, 1e-12)
    }

    @Test
    fun `normalized vector has unit magnitude`() {
        val v = Vec3(3.0, 4.0, 0.0).normalized()
        assertEquals(1.0, v.magnitude, 1e-12)
    }

    @Test
    fun `normalizing zero vector returns ZERO without throwing`() {
        val result = Vec3.ZERO.normalized()
        assertEquals(Vec3.ZERO, result)
    }

    @Test
    fun `float constructor widens to double`() {
        val v = Vec3(1.5f, 2.5f, 3.5f)
        assertEquals(1.5, v.x, 1e-6)
        assertEquals(2.5, v.y, 1e-6)
        assertEquals(3.5, v.z, 1e-6)
    }

    @Test
    fun `magnitudeSquared is dot product with self`() {
        val v = Vec3(2.0, 3.0, 6.0)
        assertEquals(v.dot(v), v.magnitudeSquared, 1e-12)
        assertEquals(49.0, v.magnitudeSquared, 1e-12)
    }
}
