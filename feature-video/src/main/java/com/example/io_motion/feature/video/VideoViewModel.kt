package com.example.io_motion.feature.video

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.io_motion.core.analysis.ExerciseAnalyzerFactory
import com.example.io_motion.core.common.models.AnalysisMode
import com.example.io_motion.core.common.models.ExerciseType
import com.example.io_motion.core.common.util.parseEnumOrDefault
import com.example.io_motion.core.pose.VideoAnalysisSession
import com.example.io_motion.core.pose.config.PoseLandmarkerConfig
import com.example.io_motion.core.pose.model.PoseModelVariant
import com.example.io_motion.data.repository.SessionRepository
import com.example.io_motion.feature.video.gallery.DeviceVideoRepository
import com.example.io_motion.feature.video.model.VideoUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class VideoViewModel @Inject constructor(
    private val videoAnalysisSession: VideoAnalysisSession,
    private val sessionRepository: SessionRepository,
    private val deviceVideoRepository: DeviceVideoRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow<VideoUiState>(VideoUiState.Idle)
    val uiState: StateFlow<VideoUiState> = _uiState.asStateFlow()

    // Read directly from the nav backstack's SavedStateHandle rather than via a separate
    // initialize() call from the composable — see LiveViewModel for the race this avoids.
    private val exerciseType = parseEnumOrDefault(savedStateHandle["exerciseType"], ExerciseType.SQUAT)
    private val modelVariant = parseEnumOrDefault(savedStateHandle["modelVariant"], PoseModelVariant.FULL)
    private var processingJob: Job? = null
    private var galleryLoadJob: Job? = null

    /** Loads the on-device video gallery. Call once permission has been granted. */
    fun openGallery() {
        galleryLoadJob?.cancel()
        _uiState.value = VideoUiState.Gallery(videos = emptyList(), isLoading = true)
        galleryLoadJob = viewModelScope.launch {
            val videos = withContext(Dispatchers.IO) { deviceVideoRepository.queryVideos() }
            _uiState.value = VideoUiState.Gallery(videos = videos, isLoading = false)
        }
    }

    fun closeGallery() {
        galleryLoadJob?.cancel()
        galleryLoadJob = null
        _uiState.value = VideoUiState.Idle
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
                            // save() schedules its own application-scoped coroutine, so the
                            // write survives even if the user taps "Analyze Another" or
                            // Cancel immediately after this, which would cancel processingJob.
                            sessionRepository.save(
                                metrics = metrics,
                                mode = AnalysisMode.OFFLINE,
                                modelVariant = modelVariant.name,
                            )
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
