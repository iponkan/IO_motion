package com.example.io_motion.core.pose.model

/**
 * Hardware delegate for MediaPipe inference.
 * [GPU] is the default; [PoseLandmarkerHelper] automatically retries with [CPU] on init failure.
 */
enum class PoseDelegate { GPU, CPU }
