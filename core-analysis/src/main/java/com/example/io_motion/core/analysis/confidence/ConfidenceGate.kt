package com.example.io_motion.core.analysis.confidence

import com.example.io_motion.core.common.models.Landmark
import com.example.io_motion.core.common.models.PoseFrame

/**
 * Guards downstream computation from unreliable landmark estimates.
 *
 * When MediaPipe confidence scores are below threshold the landmark position is unreliable
 * (occlusion, out-of-frame, motion blur). This gate returns null rather than letting bad
 * data propagate into angle or KPI computations, which would produce garbage reps.
 *
 * All methods operate on **world landmarks** because those are what the analysis engine uses
 * for angle computation.
 */
object ConfidenceGate {

    const val DEFAULT_VISIBILITY_THRESHOLD = 0.5f
    const val DEFAULT_PRESENCE_THRESHOLD = 0.5f

    /**
     * Returns the world landmark at [index] if visibility ≥ [visibilityThreshold] AND
     * presence ≥ [presenceThreshold], otherwise null.
     */
    fun getReliableLandmark(
        frame: PoseFrame,
        index: Int,
        visibilityThreshold: Float = DEFAULT_VISIBILITY_THRESHOLD,
        presenceThreshold: Float = DEFAULT_PRESENCE_THRESHOLD,
    ): Landmark? {
        val lm = frame.worldLandmarkAt(index) ?: return null
        return if (lm.visibility >= visibilityThreshold && lm.presence >= presenceThreshold) lm else null
    }

    /**
     * Returns all [indices] as a list only when every one of them individually meets the
     * confidence thresholds. Returns null if any single landmark fails.
     *
     * Use when a computation requires all landmarks to be reliable (e.g., a three-point angle).
     */
    fun getReliableLandmarks(
        frame: PoseFrame,
        indices: List<Int>,
        visibilityThreshold: Float = DEFAULT_VISIBILITY_THRESHOLD,
        presenceThreshold: Float = DEFAULT_PRESENCE_THRESHOLD,
    ): List<Landmark>? = indices.map {
        getReliableLandmark(frame, it, visibilityThreshold, presenceThreshold) ?: return null
    }

    /**
     * Returns true if every landmark at [indices] meets the confidence thresholds.
     */
    fun areReliable(
        frame: PoseFrame,
        indices: List<Int>,
        visibilityThreshold: Float = DEFAULT_VISIBILITY_THRESHOLD,
        presenceThreshold: Float = DEFAULT_PRESENCE_THRESHOLD,
    ): Boolean = getReliableLandmarks(frame, indices, visibilityThreshold, presenceThreshold) != null
}
