package com.example.io_motion.core.pose.converter

import com.example.io_motion.core.common.models.Landmark
import com.example.io_motion.core.common.models.PoseFrame
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult

/**
 * Converts a raw [PoseLandmarkerResult] from MediaPipe into the app's [PoseFrame] model.
 *
 * We take the first detected pose only ([numPoses] is always 1 in this app).
 * MediaPipe landmark visibility and presence are exposed as Java `Optional<Float>`;
 * missing values default to 0f rather than null so the rest of the pipeline can compare
 * them against thresholds without null checks.
 */
internal object PoseResultConverter {

    /**
     * @param result Raw MediaPipe result.
     * @param timestampMs The monotonic timestamp (ms) that was passed to `detectAsync`.
     * @return A [PoseFrame] for the first detected person, or `null` if no pose was found.
     */
    fun convert(result: PoseLandmarkerResult, timestampMs: Long): PoseFrame? {
        if (result.landmarks().isEmpty() || result.worldLandmarks().isEmpty()) return null

        val normalizedLandmarks = result.landmarks()[0].map { lm ->
            Landmark(
                x = lm.x(),
                y = lm.y(),
                z = lm.z(),
                visibility = lm.visibility().orElse(0f),
                presence = lm.presence().orElse(0f),
            )
        }

        val worldLandmarks = result.worldLandmarks()[0].map { lm ->
            Landmark(
                x = lm.x(),
                y = lm.y(),
                z = lm.z(),
                visibility = lm.visibility().orElse(0f),
                presence = lm.presence().orElse(0f),
            )
        }

        return PoseFrame(timestampMs, normalizedLandmarks, worldLandmarks)
    }
}
