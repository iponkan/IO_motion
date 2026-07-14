package com.example.io_motion.feature.diet

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.io_motion.core.common.models.displayName
import com.example.io_motion.core.ui.theme.IOMotionTextStyles
import com.example.io_motion.core.ui.theme.extendedColors

@Composable
fun DietPlanScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DietPlanViewModel = hiltViewModel(),
) {
    // Transient per-card confirmation: index -> "added". Reset only on leaving the screen.
    val added = remember { mutableStateMapOf<Int, Boolean>() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp)
            .padding(top = 20.dp, bottom = 36.dp),
    ) {
        BackHeader(title = "Suggested Meals", onNavigateBack = onNavigateBack)
        Spacer(Modifier.height(24.dp))

        SUGGESTED_MEALS.forEachIndexed { index, meal ->
            SuggestedMealCard(
                meal = meal,
                isAdded = added[index] == true,
                onAdd = {
                    viewModel.addToToday(meal)
                    added[index] = true
                },
            )
            Spacer(Modifier.height(14.dp))
        }
    }
}

@Composable
private fun SuggestedMealCard(meal: SuggestedMeal, isAdded: Boolean, onAdd: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.extendedColors.hairline)
            .padding(16.dp),
    ) {
        Text(text = meal.title, style = IOMotionTextStyles.cardTitle, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(4.dp))
        Text(
            text = meal.description,
            style = IOMotionTextStyles.repMeta,
            color = MaterialTheme.extendedColors.textMuted,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "${meal.kcal} KCAL · ${meal.mealType.displayName()}",
            style = IOMotionTextStyles.sessionRowMeta,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(16.dp))
        AddToTodayButton(isAdded = isAdded, onAdd = onAdd)
    }
}

@Composable
private fun AddToTodayButton(isAdded: Boolean, onAdd: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.extendedColors.hairline)
            .clickable(enabled = !isAdded, onClick = onAdd)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (isAdded) "Added ✓" else "Add to today",
            style = IOMotionTextStyles.segmentedLabel,
            color = if (isAdded) MaterialTheme.extendedColors.textMuted else MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
    }
}
