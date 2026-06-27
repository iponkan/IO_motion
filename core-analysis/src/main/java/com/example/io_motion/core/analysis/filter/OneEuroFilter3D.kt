package com.example.io_motion.core.analysis.filter

import com.example.io_motion.core.common.math.Vec3

/**
 * Three-axis wrapper around [OneEuroFilter] for smoothing 3D landmark coordinates.
 * Each axis is filtered independently with identical configuration.
 */
class OneEuroFilter3D(config: OneEuroFilterConfig = OneEuroFilterConfig()) {

    private val filterX = OneEuroFilter(config)
    private val filterY = OneEuroFilter(config)
    private val filterZ = OneEuroFilter(config)

    /**
     * Filter a 3D vector [v] at time [timestamp] (seconds).
     * First call returns [v] unchanged and initialises each axis.
     */
    fun filter(v: Vec3, timestamp: Double): Vec3 = Vec3(
        filterX.filter(v.x, timestamp),
        filterY.filter(v.y, timestamp),
        filterZ.filter(v.z, timestamp),
    )

    /** Reset all three axis filters. */
    fun reset() {
        filterX.reset()
        filterY.reset()
        filterZ.reset()
    }
}
