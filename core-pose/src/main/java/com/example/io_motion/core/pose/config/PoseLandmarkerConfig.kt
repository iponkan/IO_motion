package com.example.io_motion.core.pose.config

import com.example.io_motion.core.pose.model.PoseDelegate
import com.example.io_motion.core.pose.model.PoseModelVariant

/**
 * Runtime configuration for the MediaPipe Pose Landmarker.
 * All fields can be changed between sessions by calling [PoseFrameSource.updateConfig].
 *
 * @param modelVariant Which `.task` asset to load (Lite/Full/Heavy). Default: Full.
 * @param delegate Preferred hardware accelerator. GPU is attempted first; CPU is the fallback.
 * @param numPoses Maximum number of simultaneous pose detections. Keep at 1 for performance.
 * @param minPoseDetectionConfidence Minimum score for a detection to be considered valid.
 * @param minPosePresenceConfidence Minimum score for a pose to be considered present in the frame.
 * @param minTrackingConfidence Minimum score for tracking to continue between frames (vs re-detect).
 */
data class PoseLandmarkerConfig(
    val modelVariant: PoseModelVariant = PoseModelVariant.FULL,
    val delegate: PoseDelegate = PoseDelegate.GPU,
    val numPoses: Int = 1,
    val minPoseDetectionConfidence: Float = 0.5f,
    val minPosePresenceConfidence: Float = 0.5f,
    val minTrackingConfidence: Float = 0.5f,
)
