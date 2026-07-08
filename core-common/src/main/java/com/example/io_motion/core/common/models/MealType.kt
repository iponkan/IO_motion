package com.example.io_motion.core.common.models

/** The four meal buckets a diet entry can belong to, in display order. */
enum class MealType {
    BREAKFAST,
    LUNCH,
    DINNER,
    SNACKS,
}

fun MealType.displayName(): String = when (this) {
    MealType.BREAKFAST -> "Breakfast"
    MealType.LUNCH     -> "Lunch"
    MealType.DINNER    -> "Dinner"
    MealType.SNACKS    -> "Snacks"
}
