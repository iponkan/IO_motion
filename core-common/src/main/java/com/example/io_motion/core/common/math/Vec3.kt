package com.example.io_motion.core.common.math

import kotlin.math.sqrt

/**
 * Immutable 3D double-precision vector for pose biomechanics math.
 *
 * Using Double (not Float) throughout to avoid rounding errors when computing
 * dot products and acos on nearly-collinear vectors.
 */
data class Vec3(val x: Double, val y: Double, val z: Double) {

    constructor(x: Float, y: Float, z: Float) : this(x.toDouble(), y.toDouble(), z.toDouble())

    operator fun minus(other: Vec3) = Vec3(x - other.x, y - other.y, z - other.z)
    operator fun plus(other: Vec3) = Vec3(x + other.x, y + other.y, z + other.z)
    operator fun times(scalar: Double) = Vec3(x * scalar, y * scalar, z * scalar)
    operator fun div(scalar: Double) = Vec3(x / scalar, y / scalar, z / scalar)

    fun dot(other: Vec3): Double = x * other.x + y * other.y + z * other.z

    val magnitudeSquared: Double get() = x * x + y * y + z * z
    val magnitude: Double get() = sqrt(magnitudeSquared)

    fun normalized(): Vec3 {
        val mag = magnitude
        return if (mag < EPSILON) ZERO else this / mag
    }

    companion object {
        const val EPSILON = 1e-9
        val ZERO = Vec3(0.0, 0.0, 0.0)
    }
}
