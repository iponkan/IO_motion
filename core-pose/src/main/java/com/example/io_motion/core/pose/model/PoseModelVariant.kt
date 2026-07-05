package com.example.io_motion.core.pose.model

/**
 * Selectable MediaPipe Pose Landmarker model variants.
 *
 * All three `.task` files must be present in `core-pose/src/main/assets/models/`.
 * Run `./scripts/download_models.sh` from the project root to fetch them.
 *
 * The `models/` subdirectory is required, not cosmetic: MediaPipe's Android asset resolver
 * (`resource_util_android.cc`) `RET_CHECK`s that the path passed to `setModelAssetPath` contains
 * at least one `/`. A bare filename like `pose_landmarker_full.task` fails that check at
 * `PoseLandmarker.createFromOptions(...)` on every delegate (GPU and CPU alike) with
 * `"... doesn't have a slash in it"`, which surfaces as a fatal [PoseLandmarkerHelper] init error.
 *
 * Speed/accuracy trade-off (lower latency ↔ higher accuracy):
 *   Lite < Full < Heavy
 */
enum class PoseModelVariant(
    /** Asset path inside `core-pose/src/main/assets/`. */
    val assetFileName: String,
    val displayName: String,
) {
    LITE("models/pose_landmarker_lite.task", "Lite"),
    FULL("models/pose_landmarker_full.task", "Full"),
    HEAVY("models/pose_landmarker_heavy.task", "Heavy"),
}
