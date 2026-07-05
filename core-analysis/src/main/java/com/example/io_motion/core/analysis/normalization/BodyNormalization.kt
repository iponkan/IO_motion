package com.example.io_motion.core.analysis.normalization

import com.example.io_motion.core.common.math.AngleMath
import com.example.io_motion.core.common.math.Vec3
import com.example.io_motion.core.common.models.Landmark
import com.example.io_motion.core.common.models.PoseLandmarkIndex

/**
 * Body-relative normalization for scale-sensitive measurements.
 *
 * Joint angles are scale-invariant and need no normalization.
 * Distances and velocities are normalized by torso length (shoulder-midpoint → hip-midpoint)
 * so that results are independent of the subject's distance from the camera.
 *
 * All inputs expect world-space landmarks (metric units, hip-center origin).
 *
 * Not currently called by any analyzer — all quality scoring so far uses angles, which don't
 * need this. Reserved for a future distance- or velocity-based metric (see [BodyNormalizationTest]
 * for verified behavior in the meantime).
 */
object BodyNormalization {

    private const val MIN_TORSO_LENGTH = 0.01  // metres; guards against division by near-zero

    /**
     * Computes torso length as the Euclidean distance from the shoulder midpoint
     * to the hip midpoint in world (metric) space.
     *
     * Returns null if any of the four key landmarks (both shoulders, both hips) has
     * visibility below [visibilityThreshold], or if the computed length is implausibly small.
     */
    fun torsoLength(
        worldLandmarks: List<Landmark>,
        visibilityThreshold: Float = 0.5f,
    ): Double? {
        val ls = worldLandmarks.getOrNull(PoseLandmarkIndex.LEFT_SHOULDER) ?: return null
        val rs = worldLandmarks.getOrNull(PoseLandmarkIndex.RIGHT_SHOULDER) ?: return null
        val lh = worldLandmarks.getOrNull(PoseLandmarkIndex.LEFT_HIP) ?: return null
        val rh = worldLandmarks.getOrNull(PoseLandmarkIndex.RIGHT_HIP) ?: return null

        if (ls.visibility < visibilityThreshold || rs.visibility < visibilityThreshold ||
            lh.visibility < visibilityThreshold || rh.visibility < visibilityThreshold
        ) return null

        val shoulderMid = AngleMath.midpoint(Vec3(ls.x, ls.y, ls.z), Vec3(rs.x, rs.y, rs.z))
        val hipMid = AngleMath.midpoint(Vec3(lh.x, lh.y, lh.z), Vec3(rh.x, rh.y, rh.z))
        val length = AngleMath.distance(shoulderMid, hipMid)
        return if (length < MIN_TORSO_LENGTH) null else length
    }

    /**
     * Normalizes [value] (a distance or velocity) by the subject's torso length.
     * Returns null when torso length cannot be reliably computed.
     */
    fun normalizeByTorso(
        value: Double,
        worldLandmarks: List<Landmark>,
        visibilityThreshold: Float = 0.5f,
    ): Double? {
        val torso = torsoLength(worldLandmarks, visibilityThreshold) ?: return null
        return value / torso
    }
}
