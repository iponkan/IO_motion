package com.example.io_motion.feature.live

import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.io_motion.core.analysis.ExerciseAnalyzer
import com.example.io_motion.core.analysis.ExerciseAnalyzerFactory
import com.example.io_motion.core.analysis.model.AnalyzerState
import com.example.io_motion.core.common.models.AnalysisMode
import com.example.io_motion.core.common.models.ExerciseType
import com.example.io_motion.core.common.util.parseEnumOrDefault
import com.example.io_motion.core.pose.PoseFrameSource
import com.example.io_motion.core.pose.config.PoseLandmarkerConfig
import com.example.io_motion.core.pose.model.PoseError
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
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    // Read directly from the nav backstack's SavedStateHandle rather than via a separate
    // initialize() call from the composable: that pattern left this ViewModel briefly in its
    // default state, and LiveScreen's AndroidView factory (which reads uiState.modelVariant in
    // bindCamera) could run before a LaunchedEffect-driven initialize() call had landed, binding
    // the wrong model variant. Reading here means uiState is correct from construction.
    private val _uiState = MutableStateFlow(
        LiveUiState(
            exerciseType = parseEnumOrDefault(savedStateHandle["exerciseType"], ExerciseType.SQUAT),
            modelVariant = parseEnumOrDefault(savedStateHandle["modelVariant"], PoseModelVariant.FULL),
            // Optional guided-run args (default to "no target"/normal flow so untargeted sessions
            // are untouched). Read here rather than via a later initialize() call for the same
            // construction-time-correctness reason as exerciseType/modelVariant above.
            target = savedStateHandle.get<Int>("target") ?: LiveUiState.NO_TARGET,
            isWorkoutRun = savedStateHandle.get<Boolean>("workoutRun") ?: false,
        )
    )
    val uiState: StateFlow<LiveUiState> = _uiState.asStateFlow()

    private var analyzer: ExerciseAnalyzer? = null

    init {
        viewModelScope.launch {
            poseFrameSource.frames.collect { frameResult -> onFrameResult(frameResult) }
        }
        viewModelScope.launch {
            poseFrameSource.errors.collect { error -> onPoseError(error) }
        }
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
            sessionRepository.save(
                metrics = metrics,
                mode = AnalysisMode.LIVE,
                modelVariant = modelVariant.name,
            )
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
                // A frame only arrives once the landmarker has (re-)initialized successfully, so
                // this is a natural point to clear a previously surfaced fatal error — e.g. after
                // selectModelVariant() recovers by switching away from a variant that failed.
                fatalErrorMessage = null,
            )
        }

        // Guided-run auto-stop: once the target is met, end the session exactly as if the user
        // tapped Stop (persisting via stopSession) and surface the brief SET COMPLETE state.
        if (currentState.isSessionActive && currentState.hasTarget && targetReached(analyzerState, currentState.target)) {
            onTargetReached()
        }
    }

    private fun targetReached(state: AnalyzerState, target: Int): Boolean = when (state) {
        is AnalyzerState.Tracking     -> state.repCount >= target
        is AnalyzerState.HoldTracking -> state.validHoldMs >= target * 1_000L
        else -> false
    }

    private fun onTargetReached() {
        stopSession()
        _uiState.update { it.copy(isSetComplete = true) }
    }

    /**
     * [PoseError]s were previously only logged (see [PoseFrameSource]), so a fatal setup failure
     * (e.g. the landmarker failing to initialize on both GPU and CPU) left the user staring at a
     * camera preview with no skeleton and no explanation. Non-fatal errors are transient/recoverable
     * and are already logged upstream, so they don't need to interrupt the UI.
     */
    private fun onPoseError(error: PoseError) {
        if (!error.isFatal) return
        _uiState.update {
            it.copy(fatalErrorMessage = "Pose detection is unavailable on this device. Try restarting the session.")
        }
    }

    override fun onCleared() {
        super.onCleared()
        // viewModelScope is already cancelled by this point, so persistence must not depend on
        // it — sessionRepository.save() schedules onto its own application-scoped coroutine.
        // Without this, a session ended by system back/gesture (rather than the Stop button)
        // would be silently discarded here.
        if (_uiState.value.isSessionActive) {
            val metrics = analyzer?.finish()
            if (metrics != null) {
                sessionRepository.save(
                    metrics = metrics,
                    mode = AnalysisMode.LIVE,
                    modelVariant = _uiState.value.modelVariant.name,
                )
            }
        }
        poseFrameSource.unbindCamera()
    }
}
