package com.example.io_motion.core.analysis.filter

import kotlin.math.PI
import kotlin.math.abs

/**
 * One-Euro filter — a simple, speed-adaptive low-pass filter for noisy real-time signals.
 *
 * The filter adapts its cutoff frequency based on the estimated signal speed:
 * - Slow movements → low cutoff → strong jitter suppression.
 * - Fast movements → high cutoff → minimal lag.
 *
 * Reference: Casiez, G., Roussel, N., & Vogel, D. (2012). 1€ Filter: A simple speed-based
 * low-pass filter for noisy input in interactive systems. CHI '12, pp. 2527–2536.
 *
 * This is a scalar filter. For landmark coordinates use [OneEuroFilter3D].
 *
 * @param minCutoff Minimum cutoff frequency in Hz (default 1.0).
 * @param beta Speed coefficient — higher values reduce lag on fast signals (default 0.007).
 * @param dCutoff Cutoff for the derivative low-pass pre-filter in Hz (default 1.0).
 */
class OneEuroFilter(
    val minCutoff: Double = 1.0,
    val beta: Double = 0.007,
    val dCutoff: Double = 1.0,
) {
    private var xHatPrev: Double? = null
    private var dxHatPrev: Double = 0.0
    private var tPrev: Double? = null

    constructor(config: OneEuroFilterConfig) : this(config.minCutoff, config.beta, config.dCutoff)

    /**
     * Filter a new sample [x] at time [timestamp] (seconds).
     * The first call returns [x] unchanged and initialises internal state.
     */
    fun filter(x: Double, timestamp: Double): Double {
        val tPrevLocal = tPrev
        val xHatPrevLocal = xHatPrev

        tPrev = timestamp

        if (tPrevLocal == null || xHatPrevLocal == null) {
            xHatPrev = x
            return x
        }

        // Guard against non-advancing or reversed timestamps.
        val dt = (timestamp - tPrevLocal).coerceAtLeast(1e-6)

        // Estimate derivative from previous filtered value and filter it.
        val dx = (x - xHatPrevLocal) / dt
        val alphaD = smoothingFactor(dt, dCutoff)
        val dxHat = dxHatPrev + alphaD * (dx - dxHatPrev)
        dxHatPrev = dxHat

        // Adaptive cutoff: fast signal → higher cutoff → less smoothing.
        val cutoff = minCutoff + beta * abs(dxHat)
        val a = smoothingFactor(dt, cutoff)
        val xHat = xHatPrevLocal + a * (x - xHatPrevLocal)
        xHatPrev = xHat
        return xHat
    }

    /** Reset internal state. Call before starting a new session or after a signal discontinuity. */
    fun reset() {
        xHatPrev = null
        dxHatPrev = 0.0
        tPrev = null
    }

    // alpha = 1 / (1 + tau/dt), where tau = 1 / (2π·fc)
    private fun smoothingFactor(dt: Double, cutoff: Double): Double {
        val tau = 1.0 / (2.0 * PI * cutoff)
        return 1.0 / (1.0 + tau / dt)
    }
}
