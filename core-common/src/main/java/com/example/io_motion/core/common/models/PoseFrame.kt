package com.example.io_motion.core.common.models

/**
 * A single frame of pose detection output from MediaPipe Pose Landmarker.
 *
 * @param timestampMs Monotonically increasing timestamp in milliseconds (from CameraX or video pts)
 * @param normalizedLandmarks 33 landmarks in image-coordinate space (x/y in [0,1], z as relative depth)
 * @param worldLandmarks 33 landmarks in metric world space (x/y/z in meters, origin at hip midpoint)
 */
data class PoseFrame(
    val timestampMs: Long,
    val normalizedLandmarks: List<Landmark>,
    val worldLandmarks: List<Landmark>,
) {
    val isValid: Boolean
        get() = normalizedLandmarks.size == PoseLandmarkIndex.LANDMARK_COUNT &&
                worldLandmarks.size == PoseLandmarkIndex.LANDMARK_COUNT

    fun worldLandmarkAt(index: Int): Landmark? = worldLandmarks.getOrNull(index)
    fun normalizedLandmarkAt(index: Int): Landmark? = normalizedLandmarks.getOrNull(index)
}
