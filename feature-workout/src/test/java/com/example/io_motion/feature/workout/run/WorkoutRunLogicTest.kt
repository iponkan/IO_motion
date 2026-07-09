package com.example.io_motion.feature.workout.run

import com.example.io_motion.core.common.models.ExerciseType
import com.example.io_motion.data.model.WorkoutItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkoutRunLogicTest {

    private fun item(type: ExerciseType, sets: Int, reps: Int, position: Int) =
        WorkoutItem(id = 0, exerciseType = type, sets = sets, reps = reps, position = position)

    private fun session(type: ExerciseType, at: Long, reps: Int = 0, holdSec: Int = 0, quality: Int = 0) =
        ObservedSession(exerciseType = type, recordedAt = at, repCount = reps, holdSeconds = holdSec, qualityScore = quality)

    // ── flattenWorkout ────────────────────────────────────────────────────────────

    @Test
    fun `flattens Squat x3 Plank x3 into six ordered sets`() {
        val queue = WorkoutRunLogic.flattenWorkout(
            listOf(
                item(ExerciseType.SQUAT, sets = 3, reps = 10, position = 0),
                item(ExerciseType.PLANK, sets = 3, reps = 30, position = 1),
            )
        )
        assertEquals(6, queue.size)
        assertEquals(List(6) { it }, queue.map { it.index })
        assertEquals(
            listOf(
                ExerciseType.SQUAT, ExerciseType.SQUAT, ExerciseType.SQUAT,
                ExerciseType.PLANK, ExerciseType.PLANK, ExerciseType.PLANK,
            ),
            queue.map { it.exerciseType },
        )
        assertEquals(listOf(1, 2, 3, 1, 2, 3), queue.map { it.setNumber })
        assertEquals(listOf(3, 3, 3, 3, 3, 3), queue.map { it.totalSets })
        assertEquals(listOf(10, 10, 10, 30, 30, 30), queue.map { it.target })
    }

    @Test
    fun `respects item position when ordering the queue`() {
        val queue = WorkoutRunLogic.flattenWorkout(
            listOf(
                item(ExerciseType.PLANK, sets = 1, reps = 30, position = 1),
                item(ExerciseType.SQUAT, sets = 1, reps = 10, position = 0),
            )
        )
        assertEquals(listOf(ExerciseType.SQUAT, ExerciseType.PLANK), queue.map { it.exerciseType })
    }

    @Test
    fun `keeps duplicate exercises as independent sets`() {
        val queue = WorkoutRunLogic.flattenWorkout(
            listOf(
                item(ExerciseType.SQUAT, sets = 2, reps = 10, position = 0),
                item(ExerciseType.SQUAT, sets = 1, reps = 5, position = 1),
            )
        )
        assertEquals(3, queue.size)
        // The second item's set keeps its own target and 1/1 numbering, not merged with the first.
        assertEquals(listOf(10, 10, 5), queue.map { it.target })
        assertEquals(listOf(1, 2, 1), queue.map { it.setNumber })
        assertEquals(listOf(2, 2, 1), queue.map { it.totalSets })
    }

    @Test
    fun `empty workout flattens to empty queue`() {
        assertEquals(emptyList<PlannedSet>(), WorkoutRunLogic.flattenWorkout(emptyList()))
    }

    // ── findCompletingSession ─────────────────────────────────────────────────────

    @Test
    fun `completes on the earliest matching session at or after launch`() {
        val sessions = listOf(
            session(ExerciseType.SQUAT, at = 100),   // before launch — ignored
            session(ExerciseType.SQUAT, at = 250, reps = 10),
            session(ExerciseType.SQUAT, at = 400, reps = 12),
        )
        val match = WorkoutRunLogic.findCompletingSession(sessions, ExerciseType.SQUAT, launchedAt = 200)
        assertEquals(250L, match?.recordedAt)
        assertEquals(10, match?.repCount)
    }

    @Test
    fun `ignores sessions for other exercises`() {
        val sessions = listOf(session(ExerciseType.PUSH_UP, at = 300, reps = 8))
        assertNull(WorkoutRunLogic.findCompletingSession(sessions, ExerciseType.SQUAT, launchedAt = 200))
    }

    @Test
    fun `ignores sessions recorded before launch`() {
        val sessions = listOf(session(ExerciseType.SQUAT, at = 150, reps = 10))
        assertNull(WorkoutRunLogic.findCompletingSession(sessions, ExerciseType.SQUAT, launchedAt = 200))
    }

    @Test
    fun `matches a session recorded exactly at launch instant`() {
        val sessions = listOf(session(ExerciseType.SQUAT, at = 200, reps = 9))
        assertEquals(200L, WorkoutRunLogic.findCompletingSession(sessions, ExerciseType.SQUAT, 200)?.recordedAt)
    }

    // ── achievedFor: plank seconds vs reps ────────────────────────────────────────

    @Test
    fun `achievedFor uses reps for rep-based and hold-seconds for plank`() {
        val squatSet = PlannedSet(0, ExerciseType.SQUAT, 1, 1, target = 10)
        val plankSet = PlannedSet(1, ExerciseType.PLANK, 1, 1, target = 30)
        val squatSession = session(ExerciseType.SQUAT, at = 1, reps = 8, holdSec = 0)
        val plankSession = session(ExerciseType.PLANK, at = 1, reps = 0, holdSec = 27)
        assertEquals(8, WorkoutRunLogic.achievedFor(squatSet, squatSession))
        assertEquals(27, WorkoutRunLogic.achievedFor(plankSet, plankSession))
    }

    // ── buildSummary ──────────────────────────────────────────────────────────────

    private fun set(index: Int) = PlannedSet(index, ExerciseType.SQUAT, 1, 1, target = 10)

    @Test
    fun `summary averages quality over performed sets and counts a partial set`() {
        val results = listOf(
            SetResult(set(0), achieved = 10, quality = 90, skipped = false),
            SetResult(set(1), achieved = 4, quality = 60, skipped = false),   // partial, still performed
        )
        val summary = WorkoutRunLogic.buildSummary(results)
        assertEquals(75, summary.averageQuality)   // (90 + 60) / 2
        assertEquals(2, summary.results.size)
    }

    @Test
    fun `summary excludes skipped sets from the average`() {
        val results = listOf(
            SetResult(set(0), achieved = 10, quality = 80, skipped = false),
            SetResult(set(1), achieved = 0, quality = null, skipped = true),
        )
        assertEquals(80, WorkoutRunLogic.buildSummary(results).averageQuality)
    }

    @Test
    fun `summary average is null when every set was skipped`() {
        val results = listOf(
            SetResult(set(0), achieved = 0, quality = null, skipped = true),
            SetResult(set(1), achieved = 0, quality = null, skipped = true),
        )
        assertNull(WorkoutRunLogic.buildSummary(results).averageQuality)
    }

    // ── RestTimer ─────────────────────────────────────────────────────────────────

    @Test
    fun `rest timer starts at the default rest duration`() {
        assertEquals(WorkoutRunLogic.REST_DURATION_SEC, RestTimer.start().remainingSec)
    }

    @Test
    fun `rest timer ticks down and clamps at zero`() {
        var timer = RestTimer(2)
        assertEquals(false, timer.isFinished)
        timer = timer.tick()
        assertEquals(1, timer.remainingSec)
        timer = timer.tick()
        assertEquals(0, timer.remainingSec)
        assertEquals(true, timer.isFinished)
        timer = timer.tick()
        assertEquals(0, timer.remainingSec)   // does not go negative
    }
}
