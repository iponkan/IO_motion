package com.example.io_motion.feature.video.model

import com.example.io_motion.core.analysis.model.SessionMetrics
import com.example.io_motion.core.common.models.ExerciseType
import com.example.io_motion.core.common.models.PoseFrame
import com.example.io_motion.feature.video.gallery.DeviceVideo

sealed interface VideoUiState {
    data object Idle : VideoUiState

    data class Gallery(
        val videos: List<DeviceVideo>,
        val isLoading: Boolean,
    ) : VideoUiState

    data class Processing(
        val progress: Float,
        val framesProcessed: Int,
        val exerciseType: ExerciseType,
        val lastPoseFrame: PoseFrame?,
    ) : VideoUiState

    data class Result(
        val metrics: SessionMetrics,
        val exerciseType: ExerciseType,
    ) : VideoUiState

    data class Error(val message: String) : VideoUiState
}
