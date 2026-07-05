package com.example.io_motion.feature.live.model

import com.example.io_motion.core.analysis.model.AnalyzerState
import com.example.io_motion.core.common.models.ExerciseType
import com.example.io_motion.core.common.models.PoseFrame
import com.example.io_motion.core.pose.model.PoseModelVariant

/**
 * Immutable snapshot of everything the live-analysis screen needs to render.
 * Produced by [com.example.io_motion.feature.live.LiveViewModel] and exposed via StateFlow.
 */
data class LiveUiState(
    val exerciseType: ExerciseType = ExerciseType.SQUAT,
    val modelVariant: PoseModelVariant = PoseModelVariant.FULL,
    val isSessionActive: Boolean = false,
    val analyzerState: AnalyzerState = AnalyzerState.AwaitingStart,
    val fps: Float = 0f,
    val inferenceTimeMs: Long = 0L,
    val poseFrame: PoseFrame? = null,
    /** Live form score (0–100) derived from active form alerts. Precise per-rep quality is in session reports (Phase 7). */
    val liveFormScore: Int = 0,
    /**
     * Curated, user-facing message set when the pose engine reports a fatal error (e.g. failed
     * to initialize on both GPU and CPU delegates). Null when there is no active error.
     */
    val fatalErrorMessage: String? = null,
)
