package com.example.io_motion.core.analysis.filter

/**
 * Configuration for a [OneEuroFilter].
 *
 * @param minCutoff Minimum cutoff frequency in Hz. Lower values increase smoothing on slow signals
 *   (at the cost of more lag on sudden direction changes). Default 1.0 Hz is a good starting point
 *   for landmark coordinates; tune down (e.g. 0.5) for smoother, slower joints.
 * @param beta Speed coefficient. Higher values reduce lag on fast movements. A value near 0 gives
 *   maximum smoothing at all speeds; raise it (e.g. 0.01–0.1) when tracking fast exercise motion.
 * @param dCutoff Cutoff frequency for the internal derivative estimator in Hz. Typically left at
 *   1.0 Hz; only change if the derivative itself is too noisy.
 */
data class OneEuroFilterConfig(
    val minCutoff: Double = 1.0,
    val beta: Double = 0.007,
    val dCutoff: Double = 1.0,
)
