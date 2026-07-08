package com.example.io_motion.data.repository

import com.example.io_motion.core.common.models.MealType
import com.example.io_motion.data.model.DayLog
import kotlinx.coroutines.flow.Flow

interface DietRepository {
    /** Live-observable log for [localDate] (ISO `yyyy-MM-dd`): meals grouped by type + water cups. */
    fun dayLog(localDate: String): Flow<DayLog>

    /** Appends a food entry to [mealType] on [localDate]. Fire-and-forget (outlives the caller). */
    fun addEntry(localDate: String, mealType: MealType, name: String, kcal: Int)

    /** Removes the entry with [id]. */
    fun removeEntry(id: Long)

    /** Sets the water-cup count for [localDate]. */
    fun setWater(localDate: String, cups: Int)
}
