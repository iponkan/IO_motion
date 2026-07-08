package com.example.io_motion.data.repository

import android.util.Log
import com.example.io_motion.core.common.models.ExerciseType
import com.example.io_motion.data.dao.WorkoutDao
import com.example.io_motion.data.di.ApplicationScope
import com.example.io_motion.data.entity.WorkoutEntity
import com.example.io_motion.data.entity.WorkoutItemEntity
import com.example.io_motion.data.entity.WorkoutWithItems
import com.example.io_motion.data.model.Workout
import com.example.io_motion.data.model.WorkoutItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "WorkoutRepository"

class WorkoutRepositoryImpl @Inject constructor(
    private val dao: WorkoutDao,
    @ApplicationScope private val appScope: CoroutineScope,
) : WorkoutRepository {

    override val workouts: Flow<List<Workout>> =
        dao.observeWorkouts().map { rows -> rows.map { it.toWorkout() } }

    override suspend fun getById(id: Long): Workout? = dao.getById(id)?.toWorkout()

    override fun upsert(workout: Workout) {
        appScope.launch {
            dao.upsert(workout.toEntity(), workout.items.map { it.toEntity(workout.id) })
        }
    }

    override fun delete(id: Long) {
        appScope.launch { dao.deleteWorkout(id) }
    }
}

// ── Mappers ──────────────────────────────────────────────────────────────────

private fun Workout.toEntity() = WorkoutEntity(
    id = id,
    name = name,
    createdAt = createdAt,
    sortOrder = sortOrder,
)

private fun WorkoutItem.toEntity(workoutId: Long) = WorkoutItemEntity(
    id = id,
    workoutId = workoutId,
    exerciseType = exerciseType.name,
    sets = sets,
    reps = reps,
    position = position,
)

private fun WorkoutWithItems.toWorkout() = Workout(
    id = workout.id,
    name = workout.name,
    createdAt = workout.createdAt,
    sortOrder = workout.sortOrder,
    items = items
        .sortedBy { it.position }
        .mapNotNull { it.toItemOrNull() },
)

/**
 * Drops (rather than throwing on) a row whose stored [WorkoutItemEntity.exerciseType] no longer
 * matches an [ExerciseType] constant — same resilience rule as SessionWithReps.toRecord().
 */
private fun WorkoutItemEntity.toItemOrNull(): WorkoutItem? {
    val type = runCatching { ExerciseType.valueOf(exerciseType) }.getOrNull()
    if (type == null) {
        Log.w(TAG, "Dropping workout item $id: unknown exerciseType '$exerciseType'")
        return null
    }
    return WorkoutItem(id = id, exerciseType = type, sets = sets, reps = reps, position = position)
}
