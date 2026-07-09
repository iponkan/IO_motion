package com.example.io_motion.feature.workout.run

import com.example.io_motion.core.common.models.ExerciseType

/**
 * One set in a flattened run queue. A workout of "Squat ×3, Plank ×3" flattens to six [PlannedSet]s.
 *
 * [target] is reps for rep-based exercises and hold-seconds for [ExerciseType.PLANK] — the guided
 * runner never has to know which; it just forwards the number and the exercise type to live
 * analysis, which interprets it (matching the same reps-column-stores-seconds convention the
 * builder uses).
 *
 * @param index 0-based position in the run's full set queue.
 * @param setNumber 1-based index of this set within its exercise item (e.g. 2 of 3).
 * @param totalSets Total sets for this exercise item.
 */
data class PlannedSet(
    val index: Int,
    val exerciseType: ExerciseType,
    val setNumber: Int,
    val totalSets: Int,
    val target: Int,
) {
    val isPlank: Boolean get() = exerciseType == ExerciseType.PLANK
}

/**
 * Minimal projection of a persisted session the runner needs to detect set completion. Kept free of
 * `:data`/`:core-analysis` types so the completion-matching logic can be unit-tested on the JVM; the
 * ViewModel maps each observed `SessionRecord` to one of these.
 *
 * @param holdSeconds Valid plank hold in whole seconds (0 for rep-based sessions).
 */
data class ObservedSession(
    val exerciseType: ExerciseType,
    val recordedAt: Long,
    val repCount: Int,
    val holdSeconds: Int,
    val qualityScore: Int,
)

/**
 * Outcome of one attempted set.
 *
 * @param achieved Reps completed (or hold-seconds for plank); 0 when [skipped].
 * @param quality Session quality score of the completing session, or null when [skipped].
 */
data class SetResult(
    val plannedSet: PlannedSet,
    val achieved: Int,
    val quality: Int?,
    val skipped: Boolean,
)

/**
 * End-of-run rollup.
 *
 * @param averageQuality Mean quality over the sets that actually ran (skipped sets excluded), or
 *   null when every set was skipped.
 */
data class RunSummary(
    val results: List<SetResult>,
    val averageQuality: Int?,
)

/**
 * Pure rest-countdown helper (default 60s between sets). Modelled as an immutable value so the
 * countdown reducer can be unit-tested without coroutines/time.
 */
data class RestTimer(val remainingSec: Int) {
    val isFinished: Boolean get() = remainingSec <= 0

    /** Advances the countdown by one second, clamped at zero. */
    fun tick(): RestTimer = RestTimer((remainingSec - 1).coerceAtLeast(0))

    companion object {
        fun start(totalSec: Int = WorkoutRunLogic.REST_DURATION_SEC): RestTimer = RestTimer(totalSec)
    }
}
