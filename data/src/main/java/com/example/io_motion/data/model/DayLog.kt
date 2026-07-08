package com.example.io_motion.data.model

import com.example.io_motion.core.common.models.MealType

/** Public view of one day's diet log: meals grouped by type + water cups. */
data class DayLog(
    val localDate: String,
    val meals: Map<MealType, List<DietEntry>>,
    val waterCups: Int,
) {
    /** Total kcal logged across all meals today. */
    val totalKcal: Int get() = meals.values.sumOf { entries -> entries.sumOf { it.kcal } }

    fun kcalFor(mealType: MealType): Int = meals[mealType]?.sumOf { it.kcal } ?: 0
}

data class DietEntry(
    val id: Long,
    val mealType: MealType,
    val name: String,
    val kcal: Int,
    val loggedAt: Long,
)
