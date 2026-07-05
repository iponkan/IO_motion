package com.example.io_motion.feature.live

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.io_motion.core.common.models.AnalysisMode
import com.example.io_motion.core.common.models.ExerciseType
import com.example.io_motion.core.common.models.ThemeMode
import com.example.io_motion.core.common.models.displayName
import com.example.io_motion.core.pose.model.PoseModelVariant

@Composable
fun HomeScreen(
    onStart: (ExerciseType, PoseModelVariant, AnalysisMode) -> Unit,
    onOpenHistory: () -> Unit,
    modifier: Modifier = Modifier,
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    onCycleTheme: () -> Unit = {},
) {
    var selectedExercise by remember { mutableStateOf(ExerciseType.SQUAT) }
    var selectedModel by remember { mutableStateOf(PoseModelVariant.FULL) }
    var selectedMode by remember { mutableStateOf(AnalysisMode.LIVE) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "IO Motion",
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                )
                Text(
                    text = "AI Fitness Assessment",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onCycleTheme) {
                    Text(themeMode.displayName(), style = MaterialTheme.typography.labelLarge)
                }
                TextButton(onClick = onOpenHistory) {
                    Text("History", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        // ── Exercise selection ────────────────────────────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "Exercise", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExerciseCard("Squat",   selectedExercise == ExerciseType.SQUAT,   { selectedExercise = ExerciseType.SQUAT   }, Modifier.weight(1f))
                ExerciseCard("Sit-up",  selectedExercise == ExerciseType.SIT_UP,  { selectedExercise = ExerciseType.SIT_UP  }, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExerciseCard("Push-up", selectedExercise == ExerciseType.PUSH_UP, { selectedExercise = ExerciseType.PUSH_UP }, Modifier.weight(1f))
                ExerciseCard("Plank",   selectedExercise == ExerciseType.PLANK,   { selectedExercise = ExerciseType.PLANK   }, Modifier.weight(1f))
            }
        }

        // ── Analysis mode ─────────────────────────────────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "Analysis Mode", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedMode == AnalysisMode.LIVE,
                    onClick = { selectedMode = AnalysisMode.LIVE },
                    label = { Text("Live Camera") },
                )
                FilterChip(
                    selected = selectedMode == AnalysisMode.OFFLINE,
                    onClick = { selectedMode = AnalysisMode.OFFLINE },
                    label = { Text("Video File") },
                )
            }
        }

        // ── Model variant ─────────────────────────────────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "Model Variant", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Lite: fastest  ·  Full: balanced  ·  Heavy: most accurate",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PoseModelVariant.entries.forEach { variant ->
                    FilterChip(
                        selected = variant == selectedModel,
                        onClick = { selectedModel = variant },
                        label = { Text(variant.displayName) },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { onStart(selectedExercise, selectedModel, selectedMode) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            Text(
                text = when (selectedMode) {
                    AnalysisMode.LIVE    -> "Start Live Analysis"
                    AnalysisMode.OFFLINE -> "Select Video"
                },
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            )
        }
    }
}

@Composable
private fun ExerciseCard(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.aspectRatio(1.5f).clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
