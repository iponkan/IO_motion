package com.example.io_motion.core.pose

/**
 * Rolling-window FPS estimator.
 *
 * Keeps the last [windowSize] frame timestamps and computes FPS as
 *   (windowSize − 1) / (newest − oldest) × 1000
 * This is more stable than a per-frame delta because a single slow frame
 * doesn't immediately tank the displayed number.
 */
internal class FpsTracker(private val windowSize: Int = 30) {

    private val timestamps = ArrayDeque<Long>(windowSize)

    /** Call once per frame with the frame's submission timestamp in milliseconds. */
    fun onFrame(timestampMs: Long) {
        timestamps.addLast(timestampMs)
        while (timestamps.size > windowSize) timestamps.removeFirst()
    }

    /** Current rolling-window FPS estimate. Returns 0 until at least 2 frames are seen. */
    val fps: Float
        get() {
            if (timestamps.size < 2) return 0f
            val durationMs = timestamps.last() - timestamps.first()
            return if (durationMs <= 0L) 0f else (timestamps.size - 1) * 1000f / durationMs
        }

    fun reset() = timestamps.clear()
}
