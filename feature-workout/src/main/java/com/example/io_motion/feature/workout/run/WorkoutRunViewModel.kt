package com.example.io_motion.feature.workout.run

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.io_motion.core.common.models.ExerciseType
import com.example.io_motion.data.model.SessionRecord
import com.example.io_motion.data.repository.SessionRepository
import com.example.io_motion.data.repository.SettingsRepository
import com.example.io_motion.data.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class RunPhase {
    /** Showing the current set with START SET / Skip set. */
    READY,

    /** Counting down before the next set can start (design §7.5). */
    RESTING,

    /** All sets done — showing the per-set summary. */
    SUMMARY,
}

data class WorkoutRunUiState(
    val isLoading: Boolean = true,
    val workoutName: String = "",
    val queue: List<PlannedSet> = emptyList(),
    val currentIndex: Int = 0,
    val phase: RunPhase = RunPhase.READY,
    val restRemainingSec: Int = 0,
    val results: List<SetResult> = emptyList(),
) {
    val currentSet: PlannedSet? get() = queue.getOrNull(currentIndex)
    val summary: RunSummary get() = WorkoutRunLogic.buildSummary(results)
}

/** One-shot request to hand off to live analysis for the current set (design §7.2). */
data class LaunchLiveEvent(
    val exerciseType: ExerciseType,
    val modelVariant: String,
    val target: Int,
)

/**
 * Orchestrates a guided workout run. Pure decisions live in [WorkoutRunLogic]; this class wires them
 * to coroutines, the session observer, and navigation.
 *
 * Set completion is detected by observing [SessionRepository.sessions] rather than passing a result
 * back through the nav backstack: the data layer is the source of truth, so a set counts as done as
 * soon as a matching session is persisted (design §7.4). Process death mid-run restarts the run from
 * set 1 — an accepted limitation (documented in DESIGN_DECISIONS, Phase 7).
 */
@HiltViewModel
class WorkoutRunViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    sessionRepository: SessionRepository,
    private val settingsRepository: SettingsRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val workoutId: Long = savedStateHandle.get<Long>(ARG_WORKOUT_ID) ?: -1L

    private val _uiState = MutableStateFlow(WorkoutRunUiState())
    val uiState = _uiState.asStateFlow()

    private val _launchLive = Channel<LaunchLiveEvent>(Channel.BUFFERED)
    val launchLive = _launchLive.receiveAsFlow()

    // Epoch millis the current set was launched at; non-null only while a result is awaited. A
    // session for the current exercise recorded at/after this instant completes the set.
    private var awaitingSince: Long? = null
    private var restJob: Job? = null

    init {
        viewModelScope.launch {
            val workout = workoutRepository.getById(workoutId)
            if (workout == null) {
                _uiState.update { it.copy(isLoading = false, phase = RunPhase.SUMMARY) }
                return@launch
            }
            val queue = WorkoutRunLogic.flattenWorkout(workout.items)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    workoutName = workout.name,
                    queue = queue,
                    phase = if (queue.isEmpty()) RunPhase.SUMMARY else RunPhase.READY,
                )
            }
        }
        viewModelScope.launch {
            sessionRepository.sessions.collect { records -> onSessions(records) }
        }
    }

    fun startSet() {
        val state = _uiState.value
        val set = state.currentSet ?: return
        if (state.phase != RunPhase.READY) return
        awaitingSince = System.currentTimeMillis()
        viewModelScope.launch {
            val variant = settingsRepository.defaultModelVariant.first()
            _launchLive.send(LaunchLiveEvent(set.exerciseType, variant, set.target))
        }
    }

    fun skipSet() {
        val state = _uiState.value
        val set = state.currentSet ?: return
        if (state.phase != RunPhase.READY) return
        awaitingSince = null
        // A skipped set records no achievement and no quality, and there is no rest after it —
        // rest only follows a set that was actually performed (design §7.5).
        advance(SetResult(set, achieved = 0, quality = null, skipped = true), rest = false)
    }

    fun skipRest() {
        restJob?.cancel()
        _uiState.update { if (it.phase == RunPhase.RESTING) it.copy(phase = RunPhase.READY, restRemainingSec = 0) else it }
    }

    private fun onSessions(records: List<SessionRecord>) {
        val since = awaitingSince ?: return
        val set = _uiState.value.currentSet ?: return
        val match = WorkoutRunLogic.findCompletingSession(
            sessions = records.map { it.toObserved() },
            exerciseType = set.exerciseType,
            launchedAt = since,
        ) ?: return
        awaitingSince = null
        val achieved = WorkoutRunLogic.achievedFor(set, match)
        advance(SetResult(set, achieved = achieved, quality = match.qualityScore, skipped = false), rest = true)
    }

    /** Appends [result], then moves to the next set (with rest), or to the summary when it was the last. */
    private fun advance(result: SetResult, rest: Boolean) {
        val state = _uiState.value
        val newResults = state.results + result
        when {
            state.currentIndex >= state.queue.lastIndex ->
                _uiState.update { it.copy(results = newResults, phase = RunPhase.SUMMARY) }

            rest -> {
                _uiState.update {
                    it.copy(
                        results = newResults,
                        currentIndex = it.currentIndex + 1,
                        phase = RunPhase.RESTING,
                        restRemainingSec = WorkoutRunLogic.REST_DURATION_SEC,
                    )
                }
                startRestCountdown()
            }

            else -> _uiState.update {
                it.copy(results = newResults, currentIndex = it.currentIndex + 1, phase = RunPhase.READY)
            }
        }
    }

    private fun startRestCountdown() {
        restJob?.cancel()
        restJob = viewModelScope.launch {
            var timer = RestTimer.start()
            while (!timer.isFinished) {
                delay(1_000)
                timer = timer.tick()
                _uiState.update { it.copy(restRemainingSec = timer.remainingSec) }
            }
            _uiState.update { if (it.phase == RunPhase.RESTING) it.copy(phase = RunPhase.READY) else it }
        }
    }

    private fun SessionRecord.toObserved(): ObservedSession = ObservedSession(
        exerciseType = exerciseType,
        recordedAt = recordedAt,
        repCount = repCount,
        holdSeconds = (validHoldMs / 1_000L).toInt(),
        qualityScore = qualityScore,
    )

    companion object {
        const val ARG_WORKOUT_ID = "workoutId"
    }
}
