package com.example.io_motion.core.common.models

/**
 * A single pose landmark in 3D space with confidence scores from MediaPipe Pose Landmarker.
 *
 * @param x X coordinate — normalized 0..1 for image landmarks, meters for world landmarks
 * @param y Y coordinate — normalized 0..1 for image landmarks, meters for world landmarks
 * @param z Z coordinate — depth (same scale as x for image coords, meters for world coords)
 * @param visibility Probability [0..1] that the landmark is not occluded
 * @param presence Probability [0..1] that the landmark is within the camera frame
 */
data class Landmark(
    val x: Float,
    val y: Float,
    val z: Float,
    val visibility: Float = 0f,
    val presence: Float = 0f,
)
