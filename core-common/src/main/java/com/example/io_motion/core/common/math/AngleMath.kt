package com.example.io_motion.core.common.math

import kotlin.math.acos

/**
 * Geometric math utilities for biomechanical angle and distance calculations.
 *
 * All angles are computed from 3D world landmarks (metric space) rather than normalized
 * image landmarks, making them invariant to camera distance and subject scale.
 */
object AngleMath {

    /**
     * Computes the angle in degrees at vertex [b] formed by the rays b→a and b→c.
     *
     * Uses the dot-product formula: cos θ = (BA · BC) / (|BA| |BC|).
     * The cosine is clamped to [-1, 1] to prevent NaN from acos when floating-point
     * arithmetic produces values infinitesimally outside the valid domain.
     *
     * @return Angle in degrees [0, 180], or [Double.NaN] if either arm vector is zero-length.
     */
    fun angleDegrees(a: Vec3, b: Vec3, c: Vec3): Double {
        val ba = a - b
        val bc = c - b
        val baMag = ba.magnitude
        val bcMag = bc.magnitude
        if (baMag < Vec3.EPSILON || bcMag < Vec3.EPSILON) return Double.NaN
        val cos = (ba.dot(bc) / (baMag * bcMag)).coerceIn(-1.0, 1.0)
        return Math.toDegrees(acos(cos))
    }

    /** Euclidean distance between two points in 3D space. */
    fun distance(a: Vec3, b: Vec3): Double = (b - a).magnitude

    /** Midpoint between two 3D points. */
    fun midpoint(a: Vec3, b: Vec3): Vec3 = Vec3(
        (a.x + b.x) * 0.5,
        (a.y + b.y) * 0.5,
        (a.z + b.z) * 0.5,
    )
}
