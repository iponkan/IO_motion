package com.example.io_motion.feature.diet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.io_motion.core.common.models.MealType
import com.example.io_motion.data.model.DietEntry
import com.example.io_motion.data.repository.DietRepository
import com.example.io_motion.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.ZoneId
import javax.inject.Inject

data class DietUiState(
    val isLoading: Boolean = true,
    val totalKcal: Int = 0,
    val calorieTarget: Int = DEFAULT_CALORIE_TARGET,
    /** Calorie-ring fill fraction, clamped 0f..1f. */
    val ringFraction: Float = 0f,
    val waterCups: Int = 0,
    val waterTarget: Int = DEFAULT_WATER_TARGET,
    val meals: Map<MealType, List<DietEntry>> = emptyMap(),
    val mealKcal: Map<MealType, Int> = emptyMap(),
    /** Which meal section's Add Food sheet is open, or null when the sheet is closed. */
    val openSheetMeal: MealType? = null,
) {
    companion object {
        const val DEFAULT_CALORIE_TARGET = 2200
        const val DEFAULT_WATER_TARGET = 8
    }
}

/**
 * Drives the Diet Planning home screen. All state is keyed by today's local date (captured once per
 * screen visit); the day rolls over naturally at midnight. Non-trivial math lives in [DietMath];
 * writes go through [DietRepository] (fire-and-forget on the application scope).
 */
@HiltViewModel
class DietViewModel @Inject constructor(
    private val dietRepository: DietRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val dateKey = DietMath.dateKey(System.currentTimeMillis(), ZoneId.systemDefault())
    private val openSheetMeal = MutableStateFlow<MealType?>(null)

    val uiState: StateFlow<DietUiState> = combine(
        dietRepository.dayLog(dateKey),
        settingsRepository.calorieTarget,
        settingsRepository.waterTargetCups,
        openSheetMeal,
    ) { dayLog, calorieTarget, waterTarget, sheetMeal ->
        DietUiState(
            isLoading = false,
            totalKcal = dayLog.totalKcal,
            calorieTarget = calorieTarget,
            ringFraction = DietMath.ringFraction(dayLog.totalKcal, calorieTarget),
            waterCups = dayLog.waterCups,
            waterTarget = waterTarget,
            meals = dayLog.meals,
            mealKcal = MealType.entries.associateWith { dayLog.kcalFor(it) },
            openSheetMeal = sheetMeal,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DietUiState(),
    )

    fun stepWater(delta: Int) {
        dietRepository.setWater(dateKey, DietMath.clampWater(uiState.value.waterCups + delta))
    }

    fun removeEntry(id: Long) = dietRepository.removeEntry(id)

    fun openSheet(mealType: MealType) { openSheetMeal.value = mealType }

    fun closeSheet() { openSheetMeal.value = null }

    /** Appends the tapped preset to the meal type whose sheet is open, then closes the sheet. */
    fun selectPreset(preset: FoodPreset) {
        val meal = openSheetMeal.value ?: return
        dietRepository.addEntry(dateKey, meal, preset.name, preset.kcal)
        closeSheet()
    }
}
