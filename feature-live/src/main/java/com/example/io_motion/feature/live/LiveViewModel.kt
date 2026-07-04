package com.example.io_motion.feature.live

import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.io_motion.core.analysis.ExerciseAnalyzer
import com.example.io_motion.core.analysis.ExerciseAnalyzerFactory
import com.example.io_motion.core.analysis.model.AnalyzerState
import com.example.io_motion.core.common.models.AnalysisMode
import com.example.io_motion.core.common.models.ExerciseType
import com.example.io_motion.core.pose.PoseFrameSource
import com.example.io_motion.core.pose.config.PoseLandmarkerConfig
import com.example.io_motion.core.pose.model.PoseFrameResult
import com.example.io_motion.core.pose.model.PoseModelVariant
import com.example.io_motion.data.repository.SessionRepository
import com.example.io_motion.feature.live.model.LiveUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LiveViewModel @Inject constructor(
    private val poseFrameSource: PoseFrameSource,
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LiveUiState())
    val uiState: StateFlow<LiveUiState> = _uiState.asStateFlow()

    private var analyzer: ExerciseAnalyzer? = null
    private var initialized = false

    init {
        viewModelScope.launch {
            poseFrameSource.frames.collect { frameResult -> onFrameResult(frameResult) }
        }
    }

    fun initialize(exerciseType: ExerciseType, modelVariant: PoseModelVariant) {
        if (initialized) return
        initialized = true
        _uiState.update { it.copy(exerciseType = exerciseType, modelVariant = modelVariant) }
    }

    fun bindCamera(lifecycleOwner: LifecycleOwner, surfaceProvider: Preview.SurfaceProvider?) {
        poseFrameSource.bindCamera(
            lifecycleOwner = lifecycleOwner,
            config = PoseLandmarkerConfig(modelVariant = _uiState.value.modelVariant),
            surfaceProvider = surfaceProvider,
            lensFacing = CameraSelector.LENS_FACING_FRONT,
        )
    }

    fun startSession() {
        analyzer = ExerciseAnalyzerFactory.create(_uiState.value.exerciseType)
        _uiState.update { it.copy(isSessionActive = true, analyzerState = AnalyzerState.AwaitingStart) }
    }

    fun stopSession() {
        val metrics = analyzer?.finish()
        analyzer = null
        val modelVariant = _uiState.value.modelVariant
        _uiState.update {
            it.copy(isSessionActive = false, analyzerState = AnalyzerState.AwaitingStart, liveFormScore = 0)
        }
        if (metrics != null) {
            viewModelScope.launch {
                sessionRepository.save(
                    metrics = metrics,
                    mode = AnalysisMode.LIVE,
                    modelVariant = modelVariant.name,
                )
            }
        }
    }

    fun selectModelVariant(variant: PoseModelVariant) {
        _uiState.update { it.copy(modelVariant = variant) }
        poseFrameSource.updateConfig(PoseLandmarkerConfig(modelVariant = variant))
    }

    private fun onFrameResult(frameResult: PoseFrameResult) {
        val frame = frameResult.poseFrame
        val currentState = _uiState.value

        val analyzerState = when {
            frame == null -> AnalyzerState.LowConfidence
            currentState.isSessionActive -> analyzer?.update(frame) ?: AnalyzerState.AwaitingStart
            else -> AnalyzerState.AwaitingStart
        }

        val formScore = when (analyzerState) {
            is AnalyzerState.Tracking     -> (100 - analyzerState.alerts.size * 20).coerceAtLeast(0)
            is AnalyzerState.HoldTracking -> if (analyzerState.isFormGood) 100 else 40
            else -> 0
        }

        _uiState.update {
            it.copy(
                poseFrame = frame,
                fps = frameResult.fps,
                inferenceTimeMs = frameResult.inferenceTimeMs,
                analyzerState = analyzerState,
                liveFormScore = formScore,
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (_uiState.value.isSessionActive) analyzer?.finish()
        poseFrameSource.unbindCamera()
    }
}
