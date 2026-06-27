package com.example.io_motion.core.pose.model

/**
 * Selectable MediaPipe Pose Landmarker model variants.
 *
 * All three `.task` files must be present in `core-pose/src/main/assets/`.
 * Run `./download_models.sh` from the project root to fetch them.
 *
 * Speed/accuracy trade-off (lower latency ↔ higher accuracy):
 *   Lite < Full < Heavy
 */
enum class PoseModelVariant(
    /** Asset file name inside `core-pose/src/main/assets/`. */
    val assetFileName: String,
    val displayName: String,
) {
    LITE("pose_landmarker_lite.task", "Lite"),
    FULL("pose_landmarker_full.task", "Full"),
    HEAVY("pose_landmarker_heavy.task", "Heavy"),
}
