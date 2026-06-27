package com.example.io_motion.core.analysis.normalization

import com.example.io_motion.core.common.models.Landmark
import com.example.io_motion.core.common.models.PoseLandmarkIndex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class BodyNormalizationTest {

    private fun stub(vis: Float = 1f) = Landmark(0f, 0f, 0f, visibility = vis, presence = 1f)

    private fun makeWorldLandmarks(
        lsY: Float = 1f,
        rsY: Float = 1f,
        lhY: Float = 0f,
        rhY: Float = 0f,
        vis: Float = 1f,
    ): MutableList<Landmark> {
        val list = MutableList(PoseLandmarkIndex.LANDMARK_COUNT) { stub(vis) }
        list[PoseLandmarkIndex.LEFT_SHOULDER] = Landmark(0f, lsY, 0f, visibility = vis, presence = 1f)
        list[PoseLandmarkIndex.RIGHT_SHOULDER] = Landmark(0f, rsY, 0f, visibility = vis, presence = 1f)
        list[PoseLandmarkIndex.LEFT_HIP] = Landmark(0f, lhY, 0f, visibility = vis, presence = 1f)
        list[PoseLandmarkIndex.RIGHT_HIP] = Landmark(0f, rhY, 0f, visibility = vis, presence = 1f)
        return list
    }

    // ──────────────────────────────────────────────
    // torsoLength
    // ──────────────────────────────────────────────

    @Test
    fun `torso length matches known geometry - vertical arrangement`() {
        // Shoulders at y=1, hips at y=0 → midpoints match → distance = 1.0
        val landmarks = makeWorldLandmarks(lsY = 1f, rsY = 1f, lhY = 0f, rhY = 0f)
        val length = BodyNormalization.torsoLength(landmarks)
        assertNotNull(length)
        assertEquals(1.0, length!!, 1e-6)
    }

    @Test
    fun `torso length with offset hips - averaged midpoints`() {
        // Shoulders mid = (0, 1, 0); hips mid = (0, 0, 0) → length = 1.0
        val landmarks = makeWorldLandmarks(lsY = 1f, rsY = 1f, lhY = 0f, rhY = 0f)
        // Shift left hip to (0, 0.5, 0) and right hip to (0, -0.5, 0)
        // Hip midpoint y = 0.0 → length still 1.0
        landmarks[PoseLandmarkIndex.LEFT_HIP] = Landmark(0f, 0.5f, 0f, visibility = 1f, presence = 1f)
        landmarks[PoseLandmarkIndex.RIGHT_HIP] = Landmark(0f, -0.5f, 0f, visibility = 1f, presence = 1f)
        val length = BodyNormalization.torsoLength(landmarks)
        assertNotNull(length)
        assertEquals(1.0, length!!, 1e-6)
    }

    @Test
    fun `returns null when shoulder visibility is below threshold`() {
        val landmarks = makeWorldLandmarks(vis = 0.3f)
        assertNull(BodyNormalization.torsoLength(landmarks, visibilityThreshold = 0.5f))
    }

    @Test
    fun `returns null when hip visibility is below threshold`() {
        val landmarks = makeWorldLandmarks()
        // Drop only the hip visibility
        landmarks[PoseLandmarkIndex.LEFT_HIP] = Landmark(0f, 0f, 0f, visibility = 0.2f, presence = 1f)
        assertNull(BodyNormalization.torsoLength(landmarks, visibilityThreshold = 0.5f))
    }

    @Test
    fun `returns null when landmark list is too short`() {
        // Empty list → getOrNull returns null for all indices
        assertNull(BodyNormalization.torsoLength(emptyList()))
    }

    @Test
    fun `accepts landmarks exactly at visibility threshold`() {
        val landmarks = makeWorldLandmarks(lsY = 1f, rsY = 1f, lhY = 0f, rhY = 0f, vis = 0.5f)
        val length = BodyNormalization.torsoLength(landmarks, visibilityThreshold = 0.5f)
        assertNotNull(length)
    }

    // ──────────────────────────────────────────────
    // normalizeByTorso
    // ──────────────────────────────────────────────

    @Test
    fun `normalizeByTorso divides by torso length`() {
        // torso = 2.0 (shoulders at y=2, hips at y=0), value = 1.0 → normalized = 0.5
        val landmarks = makeWorldLandmarks(lsY = 2f, rsY = 2f, lhY = 0f, rhY = 0f)
        val normalized = BodyNormalization.normalizeByTorso(1.0, landmarks)
        assertNotNull(normalized)
        assertEquals(0.5, normalized!!, 1e-6)
    }

    @Test
    fun `normalizeByTorso returns null when confidence is low`() {
        val landmarks = makeWorldLandmarks(vis = 0.1f)
        assertNull(BodyNormalization.normalizeByTorso(1.0, landmarks, visibilityThreshold = 0.5f))
    }

    @Test
    fun `normalizing by torso is scale-invariant`() {
        // Double all coordinates → torso doubles → ratio stays at 1.0
        val landmarks = makeWorldLandmarks(lsY = 2f, rsY = 2f, lhY = 0f, rhY = 0f)
        val n1 = BodyNormalization.normalizeByTorso(2.0, landmarks)!!

        val landmarks2 = makeWorldLandmarks(lsY = 4f, rsY = 4f, lhY = 0f, rhY = 0f)
        val n2 = BodyNormalization.normalizeByTorso(4.0, landmarks2)!!

        assertEquals(n1, n2, 1e-6)
    }
}
