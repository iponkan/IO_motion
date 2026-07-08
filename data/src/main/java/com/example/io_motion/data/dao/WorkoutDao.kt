package com.example.io_motion.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.io_motion.data.entity.WorkoutEntity
import com.example.io_motion.data.entity.WorkoutItemEntity
import com.example.io_motion.data.entity.WorkoutWithItems
import kotlinx.coroutines.flow.Flow

/**
 * Abstract class (not an interface) so [upsert] can wrap its three writes in a single
 * [Transaction] — an in-place edit must delete and re-insert a workout's items atomically.
 */
@Dao
abstract class WorkoutDao {

    @Transaction
    @Query("SELECT * FROM workout_entities ORDER BY sortOrder ASC, createdAt ASC")
    abstract fun observeWorkouts(): Flow<List<WorkoutWithItems>>

    @Transaction
    @Query("SELECT * FROM workout_entities WHERE id = :id")
    abstract suspend fun getById(id: Long): WorkoutWithItems?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertWorkout(workout: WorkoutEntity): Long

    @Query("DELETE FROM workout_item_entities WHERE workoutId = :workoutId")
    abstract suspend fun deleteItemsFor(workoutId: Long)

    @Insert
    abstract suspend fun insertItems(items: List<WorkoutItemEntity>)

    @Query("DELETE FROM workout_entities WHERE id = :id")
    abstract suspend fun deleteWorkout(id: Long)

    /**
     * Inserts (id == 0) or replaces (existing id) [workout], then rewrites its items. Returns the
     * workout row id. [items] are re-stamped with that id and fresh autogen ids.
     */
    @Transaction
    open suspend fun upsert(workout: WorkoutEntity, items: List<WorkoutItemEntity>): Long {
        val workoutId = insertWorkout(workout)
        deleteItemsFor(workoutId)
        insertItems(items.map { it.copy(id = 0, workoutId = workoutId) })
        return workoutId
    }
}
