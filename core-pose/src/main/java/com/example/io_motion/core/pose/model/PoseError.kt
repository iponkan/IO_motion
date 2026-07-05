package com.example.io_motion.core.pose.model

/**
 * A [PoseLandmarkerHelper]/[PoseFrameSource] failure, surfaced to observers of
 * [PoseFrameSource.errors] so it can reach the UI instead of only being logged.
 *
 * @param message Raw diagnostic message (e.g. from MediaPipe or an underlying exception).
 *   Intended for logging, not for direct display — callers presenting this to the user should
 *   map [isFatal] to a curated, generic message rather than showing [message] verbatim.
 * @param isFatal `true` when pose detection cannot continue (e.g. the landmarker failed to
 *   initialize on both GPU and CPU delegates) as opposed to a transient, recoverable error.
 */
data class PoseError(
    val message: String,
    val isFatal: Boolean,
)
