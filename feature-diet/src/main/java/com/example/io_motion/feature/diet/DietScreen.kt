package com.example.io_motion.feature.diet

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.io_motion.core.common.models.MealType
import com.example.io_motion.core.common.models.displayName
import com.example.io_motion.core.ui.components.SectionLabel
import com.example.io_motion.core.ui.theme.IOMotionTextStyles
import com.example.io_motion.core.ui.theme.extendedColors
import com.example.io_motion.data.model.DietEntry

@Composable
fun DietScreen(
    onNavigateBack: () -> Unit,
    onOpenMealPlan: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DietViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
                .padding(top = 20.dp, bottom = 36.dp),
        ) {
            BackHeader(title = "Diet Planning", onNavigateBack = onNavigateBack)

            Spacer(Modifier.height(28.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CalorieRing(
                    totalKcal = uiState.totalKcal,
                    target = uiState.calorieTarget,
                    fraction = uiState.ringFraction,
                )
            }

            Spacer(Modifier.height(28.dp))
            WaterRow(
                cups = uiState.waterCups,
                target = uiState.waterTarget,
                onDecrement = { viewModel.stepWater(-1) },
                onIncrement = { viewModel.stepWater(1) },
            )

            Spacer(Modifier.height(28.dp))
            SectionLabel(text = "TODAY'S MEALS")
            Spacer(Modifier.height(14.dp))
            MealType.entries.forEach { meal ->
                MealSection(
                    mealType = meal,
                    entries = uiState.meals[meal].orEmpty(),
                    subtotal = uiState.mealKcal[meal] ?: 0,
                    onAddFood = { viewModel.openSheet(meal) },
                    onRemove = viewModel::removeEntry,
                )
                Spacer(Modifier.height(18.dp))
            }

            Spacer(Modifier.height(6.dp))
            MealPlanLink(onClick = onOpenMealPlan)
        }

        AddFoodSheet(
            openMeal = uiState.openSheetMeal,
            onSelect = viewModel::selectPreset,
            onDismiss = viewModel::closeSheet,
        )
    }
}

// ── Calorie ring ───────────────────────────────────────────────────────────────

@Composable
private fun CalorieRing(totalKcal: Int, target: Int, fraction: Float) {
    val trackColor = MaterialTheme.extendedColors.hairline
    val arcColor = MaterialTheme.colorScheme.primary
    Box(modifier = Modifier.size(160.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidthPx = 12.dp.toPx()
            val inset = strokeWidthPx / 2f
            val arcSize = Size(size.width - strokeWidthPx, size.height - strokeWidthPx)
            val topLeft = Offset(inset, inset)
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidthPx),
            )
            drawArc(
                color = arcColor,
                startAngle = -90f, // 12 o'clock
                sweepAngle = 360f * fraction,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = totalKcal.toString(),
                style = IOMotionTextStyles.bigNumber,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "OF $target KCAL",
                style = IOMotionTextStyles.statLabel,
                color = MaterialTheme.extendedColors.textMuted,
            )
        }
    }
}

// ── Water ──────────────────────────────────────────────────────────────────────

@Composable
private fun WaterRow(cups: Int, target: Int, onDecrement: () -> Unit, onIncrement: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.extendedColors.hairline)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Water",
                style = IOMotionTextStyles.cardTitle,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                repeat(target) { index ->
                    CupGlyph(filled = index < cups)
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        StepButton(symbol = "–", onClick = onDecrement)
        Text(
            text = "$cups/$target",
            style = IOMotionTextStyles.segmentedLabel,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(44.dp),
        )
        StepButton(symbol = "+", onClick = onIncrement)
    }
}

/** Open-top cup: 3 sides of a 2dp accent border, filled to the brim when [filled]. */
@Composable
private fun CupGlyph(filled: Boolean) {
    val accent = MaterialTheme.colorScheme.primary
    Canvas(modifier = Modifier.size(width = 16.dp, height = 20.dp)) {
        val bw = 2.dp.toPx()
        val half = bw / 2f
        val w = size.width
        val h = size.height
        if (filled) {
            drawRect(color = accent, topLeft = Offset(half, half), size = Size(w - bw, h - bw))
        }
        drawLine(accent, Offset(half, 0f), Offset(half, h), bw)          // left
        drawLine(accent, Offset(w - half, 0f), Offset(w - half, h), bw)  // right
        drawLine(accent, Offset(0f, h - half), Offset(w, h - half), bw)  // bottom
    }
}

// ── Meals ──────────────────────────────────────────────────────────────────────

@Composable
private fun MealSection(
    mealType: MealType,
    entries: List<DietEntry>,
    subtotal: Int,
    onAddFood: () -> Unit,
    onRemove: (Long) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = mealType.displayName(),
                style = IOMotionTextStyles.cardTitle,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "$subtotal kcal",
                style = IOMotionTextStyles.sessionRowMeta,
                color = MaterialTheme.extendedColors.textMuted,
            )
        }
        Spacer(Modifier.height(8.dp))
        entries.forEach { entry ->
            FoodRow(entry = entry, onRemove = { onRemove(entry.id) })
        }
        Text(
            text = "+ Add food",
            style = IOMotionTextStyles.segmentedLabel,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clickable(onClick = onAddFood)
                .padding(vertical = 10.dp),
        )
    }
}

@Composable
private fun FoodRow(entry: DietEntry, onRemove: () -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = entry.name,
                style = IOMotionTextStyles.repAngleRange,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${entry.kcal} kcal",
                style = IOMotionTextStyles.repMeta,
                color = MaterialTheme.extendedColors.textMuted,
            )
            Spacer(Modifier.width(12.dp))
            Box(
                modifier = Modifier.size(28.dp).clickable(onClick = onRemove),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Remove ${entry.name}",
                    tint = MaterialTheme.extendedColors.textMuted,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.extendedColors.hairline))
    }
}

@Composable
private fun MealPlanLink(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.extendedColors.hairline)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "View suggested meal plan",
            style = IOMotionTextStyles.cardTitle,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.extendedColors.textMuted,
        )
    }
}

// ── Add Food bottom sheet ────────────────────────────────────────────────────────

@Composable
private fun AddFoodSheet(
    openMeal: MealType?,
    onSelect: (FoodPreset) -> Unit,
    onDismiss: () -> Unit,
) {
    // Scrim (fades) + sheet (slides up); driven off whether a meal is currently open. Taps use
    // detectTapGestures rather than clickable{} so there is no ripple on the scrim/sheet surfaces.
    AnimatedVisibility(visible = openMeal != null, enter = fadeIn(), exit = fadeOut()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .pointerInput(Unit) { detectTapGestures { onDismiss() } },
        )
    }
    AnimatedVisibility(
        visible = openMeal != null,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = Modifier.fillMaxSize(),
    ) {
        // Transparent top area lets scrim taps through to dismiss; a tap on the (opaque) sheet body
        // below is consumed so it never falls through to the scrim.
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .border(1.dp, MaterialTheme.extendedColors.hairline)
                    .pointerInput(Unit) { detectTapGestures {} }
                    .navigationBarsPadding()
                    .padding(horizontal = 22.dp)
                    .padding(top = 20.dp, bottom = 24.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Add Food",
                        style = IOMotionTextStyles.screenTitle,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f),
                    )
                    Box(
                        modifier = Modifier.size(32.dp).clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                FOOD_PRESETS.forEach { preset ->
                    PresetRow(preset = preset, onClick = { onSelect(preset) })
                }
            }
        }
    }
}

@Composable
private fun PresetRow(preset: FoodPreset, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = preset.name,
            style = IOMotionTextStyles.repAngleRange,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "${preset.kcal} kcal",
            style = IOMotionTextStyles.repMeta,
            color = MaterialTheme.extendedColors.textMuted,
        )
    }
}

// ── Shared ───────────────────────────────────────────────────────────────────────

@Composable
private fun StepButton(symbol: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .border(1.dp, MaterialTheme.extendedColors.hairline)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = symbol, style = IOMotionTextStyles.exerciseRowName, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
internal fun BackHeader(title: String, onNavigateBack: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .clickable(onClick = onNavigateBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(text = title, style = IOMotionTextStyles.screenTitle, color = MaterialTheme.colorScheme.onBackground)
    }
}
