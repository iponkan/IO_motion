package com.example.io_motion.feature.diet

import com.example.io_motion.core.common.models.MealType

/** A tappable preset in the Add Food sheet (design §6). */
data class FoodPreset(val name: String, val kcal: Int)

/** Static preset list shown in the Add Food bottom sheet, in the handoff's order. */
val FOOD_PRESETS: List<FoodPreset> = listOf(
    FoodPreset("Grilled Chicken Bowl", 420),
    FoodPreset("Greek Yogurt", 150),
    FoodPreset("Protein Shake", 220),
    FoodPreset("Oatmeal & Berries", 310),
    FoodPreset("Salmon & Rice", 520),
    FoodPreset("Avocado Toast", 280),
)

/** A curated card on the Suggested Meals screen (design §7). */
data class SuggestedMeal(
    val title: String,
    val description: String,
    val kcal: Int,
    val mealType: MealType,
)

/** Static curated plan cards; "ADD TO TODAY" appends each to the matching meal type. */
val SUGGESTED_MEALS: List<SuggestedMeal> = listOf(
    SuggestedMeal("Lean Muscle Plate", "Grilled chicken, quinoa, steamed broccoli", 540, MealType.LUNCH),
    SuggestedMeal("Recovery Bowl", "Salmon, sweet potato, spinach", 610, MealType.DINNER),
    SuggestedMeal("Morning Fuel", "Egg whites, oats, banana", 380, MealType.BREAKFAST),
)
