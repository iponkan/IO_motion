package com.example.io_motion.data.repository

import com.example.io_motion.data.model.Workout
import kotlinx.coroutines.flow.Flow

interface WorkoutRepository {
    /** Live-observable list of saved workouts, ordered by sortOrder then creation time. */
    val workouts: Flow<List<Workout>>

    /** Returns the [Workout] for [id] (with its items), or null if not found. */
    suspend fun getById(id: Long): Workout?

    /**
     * Inserts a new workout (id == 0) or updates an existing one in place, rewriting its items.
     * Fire-and-forget on the application scope so the write completes even if the caller (builder
     * ViewModel) is torn down immediately after navigating back.
     */
    fun upsert(workout: Workout)

    /** Deletes the workout with [id] (cascades to its items). */
    fun delete(id: Long)
}
