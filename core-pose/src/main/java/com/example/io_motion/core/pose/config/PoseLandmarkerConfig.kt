package com.example.io_motion.core.pose.config

import com.example.io_motion.core.pose.model.PoseDelegate
import com.example.io_motion.core.pose.model.PoseModelVariant

/**
 * Runtime configuration for the MediaPipe Pose Landmarker.
 * All fields can be changed between sessions by calling [PoseFrameSource.updateConfig].
 *
 * @param modelVariant Which `.task` asset to load (Lite/Full/Heavy). Default: Full.
 * @param delegate Preferred hardware accelerator. Default: CPU.
 *
 *   GPU delegate initialization has been observed to crash the process with a native SIGSEGV
 *   on some devices — a hard crash happens before any JVM exception is thrown, bypassing both
 *   [PoseLandmarkerHelper.setup]'s synchronous try/catch and its async error-listener fallback,
 *   neither of which can catch a native-level crash. Defaulting to CPU avoids the crash outright;
 *   callers can still opt into GPU explicitly once its device compatibility is better understood.
 * @param numPoses Maximum number of simultaneous pose detections. Keep at 1 for performance.
 * @param minPoseDetectionConfidence Minimum score for a detection to be considered valid.
 * @param minPosePresenceConfidence Minimum score for a pose to be considered present in the frame.
 * @param minTrackingConfidence Minimum score for tracking to continue between frames (vs re-detect).
 */
data class PoseLandmarkerConfig(
    val modelVariant: PoseModelVariant = PoseModelVariant.FULL,
    val delegate: PoseDelegate = PoseDelegate.CPU,
    val numPoses: Int = 1,
    val minPoseDetectionConfidence: Float = 0.5f,
    val minPosePresenceConfidence: Float = 0.5f,
    val minTrackingConfidence: Float = 0.5f,
)
