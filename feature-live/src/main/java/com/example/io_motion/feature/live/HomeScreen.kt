package com.example.io_motion.feature.live

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.example.io_motion.core.common.models.AnalysisMode
import com.example.io_motion.core.common.models.ExerciseType
import com.example.io_motion.core.common.models.ThemeMode
import com.example.io_motion.core.common.models.displayName
import com.example.io_motion.core.pose.model.PoseModelVariant
import com.example.io_motion.core.ui.theme.Accent
import com.example.io_motion.core.ui.theme.CutCorner
import com.example.io_motion.core.ui.theme.IOMotionTextStyles
import com.example.io_motion.core.ui.theme.LocalCutCornerEnabled
import com.example.io_motion.core.ui.theme.cutCornerShape
import com.example.io_motion.core.ui.theme.extendedColors

@Composable
fun HomeScreen(
    onStart: (ExerciseType, PoseModelVariant, AnalysisMode) -> Unit,
    onOpenHistory: () -> Unit,
    modifier: Modifier = Modifier,
    themeMode: ThemeMode = ThemeMode.DARK,
    onToggleTheme: () -> Unit = {},
) {
    var selectedExercise by remember { mutableStateOf(ExerciseType.SQUAT) }
    var selectedModel by remember { mutableStateOf(PoseModelVariant.FULL) }
    var selectedMode by remember { mutableStateOf(AnalysisMode.LIVE) }
    val cutCornerEnabled = LocalCutCornerEnabled.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp)
            .padding(top = 20.dp, bottom = 40.dp),
    ) {
        // ── Wordmark + theme/history controls ───────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.graphicsLayer { rotationZ = -4f }) {
                Row {
                    Text(text = "IO", style = IOMotionTextStyles.wordmark, color = Accent)
                    Text(text = "Motion", style = IOMotionTextStyles.wordmark, color = MaterialTheme.colorScheme.onBackground)
                }
                Text(
                    text = "AI FITNESS ASSESSMENT",
                    style = IOMotionTextStyles.subtitle,
                    color = MaterialTheme.extendedColors.textMuted,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onToggleTheme),
                    contentAlignment = Alignment.Center,
                ) {
                    val (icon, description) = when (themeMode) {
                        ThemeMode.DARK  -> Icons.Outlined.WbSunny to "Switch to light theme"
                        ThemeMode.LIGHT -> Icons.Outlined.DarkMode to "Switch to dark theme"
                    }
                    Icon(imageVector = icon, contentDescription = description, tint = MaterialTheme.colorScheme.onBackground)
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onOpenHistory),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(imageVector = Icons.Outlined.History, contentDescription = "Session history", tint = MaterialTheme.colorScheme.onBackground)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // ── Exercise selection ────────────────────────────────────────────────
        SectionLabel(text = "EXERCISE")
        Spacer(modifier = Modifier.height(14.dp))
        ExerciseList(
            exercises = ExerciseType.entries,
            selected = selectedExercise,
            onSelect = { selectedExercise = it },
            cutCornerEnabled = cutCornerEnabled,
        )

        Spacer(modifier = Modifier.height(28.dp))

        // ── Analysis mode ─────────────────────────────────────────────────────
        SectionLabel(text = "ANALYSIS MODE")
        Spacer(modifier = Modifier.height(14.dp))
        SegmentedControl(
            options = listOf("Live Camera", "Video File"),
            selectedIndex = if (selectedMode == AnalysisMode.LIVE) 0 else 1,
            onSelect = { index -> selectedMode = if (index == 0) AnalysisMode.LIVE else AnalysisMode.OFFLINE },
            cutCornerEnabled = cutCornerEnabled,
        )

        Spacer(modifier = Modifier.height(28.dp))

        // ── Model variant ─────────────────────────────────────────────────────
        SectionLabel(text = "MODEL VARIANT")
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "Lite: fastest · Full: balanced · Heavy: most accurate",
            style = IOMotionTextStyles.modelVariantCaption,
            color = MaterialTheme.extendedColors.textMuted,
        )
        Spacer(modifier = Modifier.height(10.dp))
        SegmentedControl(
            options = PoseModelVariant.entries.map { it.displayName },
            selectedIndex = PoseModelVariant.entries.indexOf(selectedModel),
            onSelect = { index -> selectedModel = PoseModelVariant.entries[index] },
            cutCornerEnabled = cutCornerEnabled,
        )

        Spacer(modifier = Modifier.height(36.dp))

        // ── CTA ──────────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .background(Accent, cutCornerShape(CutCorner.ctaButton, cutCornerEnabled))
                .clickable { onStart(selectedExercise, selectedModel, selectedMode) },
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = when (selectedMode) {
                        AnalysisMode.LIVE    -> "START LIVE ANALYSIS"
                        AnalysisMode.OFFLINE -> "SELECT VIDEO"
                    },
                    style = IOMotionTextStyles.ctaLabel,
                    color = MaterialTheme.extendedColors.accentOn,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.extendedColors.accentOn,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .background(Accent),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = IOMotionTextStyles.sectionLabel,
            color = MaterialTheme.extendedColors.textMuted,
        )
    }
}

/**
 * Vertical exercise picker whose selection highlight slides between rows, the same way the
 * segmented-control indicator slides between options, instead of instantly swapping colors.
 */
@Composable
private fun ExerciseList(
    exercises: List<ExerciseType>,
    selected: ExerciseType,
    onSelect: (ExerciseType) -> Unit,
    cutCornerEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val rowHeight = 64.dp
    val selectedIndex = exercises.indexOf(selected)
    val indicatorOffset by animateDpAsState(
        targetValue = rowHeight * selectedIndex,
        animationSpec = tween(250),
        label = "exerciseIndicatorOffset",
    )

    Box(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(rowHeight)
                .offset(y = indicatorOffset)
                .background(Accent, cutCornerShape(CutCorner.selectedRow, cutCornerEnabled)),
        )
        Column {
            exercises.forEachIndexed { index, exercise ->
                ExerciseRow(
                    label = exercise.displayName(),
                    selected = exercise == selected,
                    onClick = { onSelect(exercise) },
                    showDivider = index != exercises.lastIndex,
                )
            }
        }
    }
}

@Composable
private fun ExerciseRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    showDivider: Boolean,
    modifier: Modifier = Modifier,
) {
    val labelColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.extendedColors.accentOn else MaterialTheme.colorScheme.onBackground,
        label = "exerciseLabelColor",
    )
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clickable(onClick = onClick)
                .padding(horizontal = 18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = label, style = IOMotionTextStyles.exerciseRowName, color = labelColor)
                if (selected) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = "Selected",
                        tint = labelColor,
                    )
                }
            }
        }
        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.extendedColors.hairline),
            )
        }
    }
}

@Composable
private fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    cutCornerEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .border(1.dp, MaterialTheme.extendedColors.segmentedTrackBorder),
    ) {
        val segmentWidth = maxWidth / options.size
        val indicatorOffset by animateDpAsState(
            targetValue = segmentWidth * selectedIndex,
            animationSpec = tween(250),
            label = "segmentIndicatorOffset",
        )
        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(segmentWidth)
                .fillMaxHeight()
                .background(Accent, cutCornerShape(CutCorner.segmentedIndicator, cutCornerEnabled)),
        )
        Row(modifier = Modifier.fillMaxSize()) {
            options.forEachIndexed { index, label ->
                val active = index == selectedIndex
                val labelColor by animateColorAsState(
                    targetValue = if (active) MaterialTheme.extendedColors.accentOn else MaterialTheme.extendedColors.textMuted,
                    label = "segmentLabelColor",
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onSelect(index) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = label, style = IOMotionTextStyles.segmentedLabel, color = labelColor)
                }
            }
        }
    }
}
