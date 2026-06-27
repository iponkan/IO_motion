package com.example.io_motion.core.pose.model

import com.example.io_motion.core.common.models.PoseFrame

/**
 * One camera frame worth of pose analysis output, enriched with timing metrics for the UI.
 *
 * @param poseFrame Detected pose landmarks, or `null` when no person is in frame.
 * @param inferenceTimeMs Wall-clock time taken by the MediaPipe inference call in milliseconds.
 * @param fps Rolling-window frames-per-second estimate for the live display badge.
 */
data class PoseFrameResult(
    val poseFrame: PoseFrame?,
    val inferenceTimeMs: Long,
    val fps: Float,
)
