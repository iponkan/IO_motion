package com.example.io_motion.feature.workout.run

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.LaunchedEffect
import com.example.io_motion.core.common.models.ExerciseType
import com.example.io_motion.core.common.models.displayName
import com.example.io_motion.core.ui.components.PrimaryButton
import com.example.io_motion.core.ui.components.SectionLabel
import com.example.io_motion.core.ui.theme.IOMotionTextStyles
import com.example.io_motion.core.ui.theme.extendedColors
import com.example.io_motion.core.ui.theme.scoreColor
import com.example.io_motion.feature.workout.list.BackHeader

@Composable
fun WorkoutRunScreen(
    onNavigateBack: () -> Unit,
    onLaunchLive: (exerciseType: ExerciseType, modelVariant: String, target: Int) -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WorkoutRunViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.launchLive.collect { event ->
            onLaunchLive(event.exerciseType, event.modelVariant, event.target)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 22.dp)
            .padding(top = 20.dp, bottom = 36.dp),
    ) {
        BackHeader(
            title = uiState.workoutName.ifBlank { "Run Workout" },
            onNavigateBack = onNavigateBack,
        )
        Spacer(Modifier.height(24.dp))

        when {
            uiState.isLoading -> Unit
            uiState.phase == RunPhase.SUMMARY ->
                SummaryContent(uiState = uiState, onDone = onFinish, modifier = Modifier.weight(1f))
            uiState.phase == RunPhase.RESTING ->
                RestContent(uiState = uiState, onSkipRest = viewModel::skipRest, modifier = Modifier.weight(1f))
            else ->
                ReadyContent(
                    uiState = uiState,
                    onStartSet = viewModel::startSet,
                    onSkipSet = viewModel::skipSet,
                    modifier = Modifier.weight(1f),
                )
        }
    }
}

// ── READY ────────────────────────────────────────────────────────────────────────

@Composable
private fun ReadyContent(
    uiState: WorkoutRunUiState,
    onStartSet: () -> Unit,
    onSkipSet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val set = uiState.currentSet ?: return
    Column(modifier = modifier.fillMaxWidth()) {
        SectionLabel(text = "UP NEXT")
        Spacer(Modifier.height(14.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.extendedColors.hairline)
                .padding(20.dp),
        ) {
            Column {
                Text(
                    text = set.exerciseType.displayName(),
                    style = IOMotionTextStyles.exerciseRowName,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = setSubtitle(set),
                    style = IOMotionTextStyles.sessionRowMeta,
                    color = MaterialTheme.extendedColors.textMuted,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = "SET ${set.index + 1} OF ${uiState.queue.size}",
            style = IOMotionTextStyles.statLabel,
            color = MaterialTheme.extendedColors.textMuted,
        )

        Spacer(Modifier.weight(1f))
        PrimaryButton(text = "Start Set", onClick = onStartSet)
        Spacer(Modifier.height(12.dp))
        GhostButton(text = "Skip set", onClick = onSkipSet)
    }
}

// ── RESTING ──────────────────────────────────────────────────────────────────────

@Composable
private fun RestContent(
    uiState: WorkoutRunUiState,
    onSkipRest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val next = uiState.currentSet
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        Text(
            text = "REST",
            style = IOMotionTextStyles.statLabel,
            color = MaterialTheme.extendedColors.textMuted,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = formatRest(uiState.restRemainingSec),
            style = IOMotionTextStyles.scoreNumber,
            color = MaterialTheme.colorScheme.primary,
        )
        if (next != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "NEXT — ${next.exerciseType.displayName().uppercase()} · SET ${next.setNumber} OF ${next.totalSets}",
                style = IOMotionTextStyles.sessionRowMeta,
                color = MaterialTheme.extendedColors.textMuted,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.weight(1f))
        GhostButton(text = "Skip rest", onClick = onSkipRest)
    }
}

// ── SUMMARY ──────────────────────────────────────────────────────────────────────

@Composable
private fun SummaryContent(
    uiState: WorkoutRunUiState,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val summary = uiState.summary
    Column(modifier = modifier.fillMaxWidth()) {
        SectionLabel(text = "WORKOUT COMPLETE")
        Spacer(Modifier.height(14.dp))

        if (summary.results.isEmpty()) {
            Text(
                text = "This workout has no sets to run.",
                style = IOMotionTextStyles.modelVariantCaption,
                color = MaterialTheme.extendedColors.textMuted,
            )
        } else {
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${summary.results.count { !it.skipped }}/${summary.results.size}",
                        style = IOMotionTextStyles.bigNumber,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = "SETS DONE",
                        style = IOMotionTextStyles.statLabel,
                        color = MaterialTheme.extendedColors.textMuted,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = summary.averageQuality?.toString() ?: "—",
                        style = IOMotionTextStyles.bigNumber,
                        color = summary.averageQuality
                            ?.let { MaterialTheme.extendedColors.scoreColor(it) }
                            ?: MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = "AVG QUALITY",
                        style = IOMotionTextStyles.statLabel,
                        color = MaterialTheme.extendedColors.textMuted,
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(summary.results) { result -> SetResultRow(result) }
            }
        }

        Spacer(Modifier.height(16.dp))
        PrimaryButton(text = "Done", onClick = onDone)
    }
}

@Composable
private fun SetResultRow(result: SetResult) {
    val set = result.plannedSet
    val unit = if (set.isPlank) "s" else " reps"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.extendedColors.hairline)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${set.exerciseType.displayName()} · Set ${set.setNumber}",
                style = IOMotionTextStyles.cardTitle,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (result.skipped) "Skipped" else "${result.achieved}$unit of ${set.target}$unit",
                style = IOMotionTextStyles.repMeta,
                color = MaterialTheme.extendedColors.textMuted,
            )
        }
        Text(
            text = result.quality?.toString() ?: "—",
            style = IOMotionTextStyles.repScore,
            color = result.quality
                ?.let { MaterialTheme.extendedColors.scoreColor(it) }
                ?: MaterialTheme.extendedColors.textMuted,
        )
    }
}

// ── Shared ───────────────────────────────────────────────────────────────────────

@Composable
private fun GhostButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.extendedColors.hairline)
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text.uppercase(),
            style = IOMotionTextStyles.segmentedLabel,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

private fun setSubtitle(set: PlannedSet): String {
    val target = if (set.isPlank) "${set.target} SEC HOLD" else "${set.target} REPS"
    return "SET ${set.setNumber} OF ${set.totalSets} · $target"
}

private fun formatRest(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)
