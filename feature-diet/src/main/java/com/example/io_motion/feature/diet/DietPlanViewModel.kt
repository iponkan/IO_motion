package com.example.io_motion.feature.diet

import androidx.lifecycle.ViewModel
import com.example.io_motion.data.repository.DietRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.ZoneId
import javax.inject.Inject

/**
 * Backs the Suggested Meals screen. Adding a plan appends it to today's log via [DietRepository]
 * without navigating away; the button's confirmation state is transient UI state held in the
 * composable. Keyed by the same today's-date derivation as [DietViewModel].
 */
@HiltViewModel
class DietPlanViewModel @Inject constructor(
    private val dietRepository: DietRepository,
) : ViewModel() {

    private val dateKey = DietMath.dateKey(System.currentTimeMillis(), ZoneId.systemDefault())

    fun addToToday(meal: SuggestedMeal) {
        dietRepository.addEntry(dateKey, meal.mealType, meal.title, meal.kcal)
    }
}
