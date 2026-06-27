package com.example.io_motion.core.analysis.confidence

import com.example.io_motion.core.common.models.Landmark
import com.example.io_motion.core.common.models.PoseLandmarkIndex
import com.example.io_motion.core.common.models.PoseFrame
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class ConfidenceGateTest {

    private fun landmark(vis: Float, pres: Float = 1f) =
        Landmark(0f, 0f, 0f, visibility = vis, presence = pres)

    private fun makeFrame(worldLandmarks: List<Landmark>): PoseFrame =
        PoseFrame(0L, emptyList(), worldLandmarks)

    private fun reliableFrame(): PoseFrame {
        val lms = MutableList(PoseLandmarkIndex.LANDMARK_COUNT) { landmark(1f) }
        return makeFrame(lms)
    }

    private fun frameWithLandmarkAt(index: Int, lm: Landmark): PoseFrame {
        val lms = MutableList(PoseLandmarkIndex.LANDMARK_COUNT) { landmark(1f) }
        lms[index] = lm
        return makeFrame(lms)
    }

    // ──────────────────────────────────────────────
    // getReliableLandmark
    // ──────────────────────────────────────────────

    @Test
    fun `returns landmark when both visibility and presence exceed threshold`() {
        val frame = frameWithLandmarkAt(PoseLandmarkIndex.LEFT_KNEE, landmark(vis = 0.9f, pres = 0.9f))
        assertNotNull(ConfidenceGate.getReliableLandmark(frame, PoseLandmarkIndex.LEFT_KNEE))
    }

    @Test
    fun `returns null when visibility is below threshold`() {
        val frame = frameWithLandmarkAt(PoseLandmarkIndex.LEFT_KNEE, landmark(vis = 0.3f, pres = 1.0f))
        assertNull(ConfidenceGate.getReliableLandmark(frame, PoseLandmarkIndex.LEFT_KNEE))
    }

    @Test
    fun `returns null when presence is below threshold`() {
        val frame = frameWithLandmarkAt(PoseLandmarkIndex.LEFT_KNEE, landmark(vis = 1.0f, pres = 0.1f))
        assertNull(ConfidenceGate.getReliableLandmark(frame, PoseLandmarkIndex.LEFT_KNEE))
    }

    @Test
    fun `returns null for out-of-bounds landmark index`() {
        val frame = reliableFrame()
        assertNull(ConfidenceGate.getReliableLandmark(frame, 99))
    }

    @Test
    fun `returns null for empty world landmark list`() {
        val frame = PoseFrame(0L, emptyList(), emptyList())
        assertNull(ConfidenceGate.getReliableLandmark(frame, 0))
    }

    @Test
    fun `accepts landmark exactly at threshold`() {
        val frame = frameWithLandmarkAt(
            PoseLandmarkIndex.LEFT_HIP,
            landmark(vis = 0.5f, pres = 0.5f),
        )
        assertNotNull(
            ConfidenceGate.getReliableLandmark(
                frame, PoseLandmarkIndex.LEFT_HIP,
                visibilityThreshold = 0.5f,
                presenceThreshold = 0.5f,
            )
        )
    }

    // ──────────────────────────────────────────────
    // getReliableLandmarks (all-or-nothing)
    // ──────────────────────────────────────────────

    @Test
    fun `returns list when all requested landmarks are reliable`() {
        val frame = reliableFrame()
        val indices = listOf(
            PoseLandmarkIndex.LEFT_HIP,
            PoseLandmarkIndex.LEFT_KNEE,
            PoseLandmarkIndex.LEFT_ANKLE,
        )
        val result = ConfidenceGate.getReliableLandmarks(frame, indices)
        assertNotNull(result)
        assertTrue(result!!.size == 3)
    }

    @Test
    fun `returns null if any single landmark fails confidence check`() {
        val frame = frameWithLandmarkAt(
            PoseLandmarkIndex.LEFT_KNEE,
            landmark(vis = 0.1f, pres = 1.0f),
        )
        val indices = listOf(
            PoseLandmarkIndex.LEFT_HIP,
            PoseLandmarkIndex.LEFT_KNEE,  // this one fails
            PoseLandmarkIndex.LEFT_ANKLE,
        )
        assertNull(ConfidenceGate.getReliableLandmarks(frame, indices))
    }

    @Test
    fun `returns empty list for empty indices`() {
        val frame = reliableFrame()
        val result = ConfidenceGate.getReliableLandmarks(frame, emptyList())
        assertNotNull(result)
        assertTrue(result!!.isEmpty())
    }

    // ──────────────────────────────────────────────
    // areReliable
    // ──────────────────────────────────────────────

    @Test
    fun `areReliable returns true when all landmarks pass`() {
        val frame = reliableFrame()
        assertTrue(
            ConfidenceGate.areReliable(
                frame,
                listOf(PoseLandmarkIndex.LEFT_SHOULDER, PoseLandmarkIndex.RIGHT_SHOULDER),
            )
        )
    }

    @Test
    fun `areReliable returns false when any landmark fails`() {
        val frame = frameWithLandmarkAt(
            PoseLandmarkIndex.RIGHT_SHOULDER,
            landmark(vis = 0.0f, pres = 0.0f),
        )
        assertFalse(
            ConfidenceGate.areReliable(
                frame,
                listOf(PoseLandmarkIndex.LEFT_SHOULDER, PoseLandmarkIndex.RIGHT_SHOULDER),
            )
        )
    }
}
