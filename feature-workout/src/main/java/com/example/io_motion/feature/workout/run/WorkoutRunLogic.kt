package com.example.io_motion.feature.workout.run

import com.example.io_motion.core.common.models.ExerciseType
import com.example.io_motion.data.model.WorkoutItem

/**
 * Pure, JVM-testable core of the guided workout runner: queue flattening, set-completion matching,
 * and the end-of-run summary. All time inputs are primitives so this stays free of Android and can
 * be exhaustively unit-tested. Orchestration (coroutines, navigation, the session observer wiring)
 * lives in [WorkoutRunViewModel].
 */
object WorkoutRunLogic {

    /** Default rest between sets, in seconds (design §7.5). */
    const val REST_DURATION_SEC = 60

    /**
     * Flattens a workout's items (in [WorkoutItem.position] order) into the ordered queue of
     * individual sets the runner walks through, e.g. `[(Squat 1/3), (Squat 2/3), (Squat 3/3),
     * (Plank 1/3), ...]`. Duplicate exercises stay as independent items and produce independent sets.
     */
    fun flattenWorkout(items: List<WorkoutItem>): List<PlannedSet> {
        val queue = ArrayList<PlannedSet>()
        var index = 0
        for (item in items.sortedBy { it.position }) {
            for (setNumber in 1..item.sets) {
                queue += PlannedSet(
                    index = index++,
                    exerciseType = item.exerciseType,
                    setNumber = setNumber,
                    totalSets = item.sets,
                    target = item.reps,
                )
            }
        }
        return queue
    }

    /**
     * Finds the session that completes the current set. A set launched at [launchedAt] is completed
     * by the earliest session for the same [exerciseType] recorded at or after that moment — the
     * data layer, not a nav result, is the source of truth (design §7.4). Sessions from earlier in
     * the run (or from unrelated history) are naturally excluded because they predate [launchedAt].
     */
    fun findCompletingSession(
        sessions: List<ObservedSession>,
        exerciseType: ExerciseType,
        launchedAt: Long,
    ): ObservedSession? = sessions
        .filter { it.exerciseType == exerciseType && it.recordedAt >= launchedAt }
        .minByOrNull { it.recordedAt }

    /** Achieved count for a completed [set]: hold-seconds for plank, valid reps otherwise. */
    fun achievedFor(set: PlannedSet, session: ObservedSession): Int =
        if (set.isPlank) session.holdSeconds else session.repCount

    /** Rolls per-set results into the run summary, averaging quality over the sets that actually ran. */
    fun buildSummary(results: List<SetResult>): RunSummary {
        val qualities = results.filterNot { it.skipped }.mapNotNull { it.quality }
        val average = if (qualities.isEmpty()) null else qualities.sum() / qualities.size
        return RunSummary(results = results, averageQuality = average)
    }
}
