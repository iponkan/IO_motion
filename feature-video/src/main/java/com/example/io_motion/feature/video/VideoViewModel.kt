package com.example.io_motion.feature.video

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.io_motion.core.analysis.ExerciseAnalyzerFactory
import com.example.io_motion.core.common.models.ExerciseType
import com.example.io_motion.core.pose.VideoAnalysisSession
import com.example.io_motion.core.pose.config.PoseLandmarkerConfig
import com.example.io_motion.core.pose.model.PoseModelVariant
import com.example.io_motion.feature.video.model.VideoUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VideoViewModel @Inject constructor(
    private val videoAnalysisSession: VideoAnalysisSession,
) : ViewModel() {

    private val _uiState = MutableStateFlow<VideoUiState>(VideoUiState.Idle)
    val uiState: StateFlow<VideoUiState> = _uiState.asStateFlow()

    private var exerciseType = ExerciseType.SQUAT
    private var modelVariant = PoseModelVariant.FULL
    private var initialized = false
    private var processingJob: Job? = null

    fun initialize(exerciseType: ExerciseType, modelVariant: PoseModelVariant) {
        if (initialized) return
        initialized = true
        this.exerciseType = exerciseType
        this.modelVariant = modelVariant
    }

    fun processVideo(uri: Uri) {
        processingJob?.cancel()
        val analyzer = ExerciseAnalyzerFactory.create(exerciseType)
        var framesProcessed = 0

        processingJob = viewModelScope.launch {
            videoAnalysisSession
                .process(uri, PoseLandmarkerConfig(modelVariant = modelVariant))
                .collect { event ->
                    when (event) {
                        is VideoAnalysisSession.ProgressEvent.Frame -> {
                            event.poseFrame?.let { analyzer.update(it) }
                            framesProcessed++
                            _uiState.value = VideoUiState.Processing(
                                progress = event.progress,
                                framesProcessed = framesProcessed,
                                exerciseType = exerciseType,
                                lastPoseFrame = event.poseFrame,
                            )
                        }
                        is VideoAnalysisSession.ProgressEvent.Complete -> {
                            val metrics = analyzer.finish()
                            _uiState.value = VideoUiState.Result(metrics, exerciseType)
                        }
                        is VideoAnalysisSession.ProgressEvent.Error -> {
                            _uiState.value = VideoUiState.Error(event.message)
                        }
                    }
                }
        }
    }

    fun cancelProcessing() {
        processingJob?.cancel()
        processingJob = null
        _uiState.value = VideoUiState.Idle
    }

    fun reset() {
        processingJob?.cancel()
        processingJob = null
        _uiState.value = VideoUiState.Idle
    }
}
