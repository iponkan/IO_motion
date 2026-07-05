package com.example.io_motion.feature.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.io_motion.core.common.models.ExerciseType
import com.example.io_motion.core.common.models.displayName
import com.example.io_motion.core.common.models.metaLabel
import com.example.io_motion.core.ui.theme.Accent
import com.example.io_motion.core.ui.theme.IOMotionTextStyles
import com.example.io_motion.core.ui.theme.extendedColors
import com.example.io_motion.core.ui.theme.scoreColor
import com.example.io_motion.data.model.SessionRecord

@Composable
fun HistoryScreen(
    onNavigateBack: () -> Unit,
    onOpenReport: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = modifier.fillMaxSize().statusBarsPadding()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(20.dp).clickable(onClick = onNavigateBack),
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Session History",
                style = IOMotionTextStyles.historyTitle,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Accent)
            }
            uiState.sessions.isEmpty() -> EmptyState(Modifier.fillMaxSize())
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 22.dp),
            ) {
                items(uiState.sessions, key = { it.id }) { record ->
                    SessionRow(
                        record = record,
                        onClick = { onOpenReport(record.id) },
                        onDelete = { viewModel.delete(record.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionRow(
    record: SessionRecord,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val metrics = record.metrics
    val isPlank = metrics.exerciseType == ExerciseType.PLANK

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 20.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = metrics.exerciseType.displayName(),
                    style = IOMotionTextStyles.sessionRowName,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${record.analysisMode.metaLabel()} · ${record.modelVariant.uppercase()}",
                    style = IOMotionTextStyles.sessionRowMeta,
                    color = MaterialTheme.extendedColors.textMutedSecondary,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = record.recordedAt.toDateString(),
                    style = IOMotionTextStyles.metaTimestamp,
                    color = MaterialTheme.extendedColors.textMuted,
                )
                Spacer(Modifier.height(8.dp))
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = "Delete session",
                    tint = MaterialTheme.extendedColors.textMuted.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp).clickable(onClick = onDelete),
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(34.dp)) {
            StatPair("${metrics.sessionQualityScore}", "QUALITY", MaterialTheme.extendedColors.scoreColor(metrics.sessionQualityScore))
            if (isPlank) {
                val holdSec = metrics.validHoldMs / 1_000L
                StatPair("%d:%02d".format(holdSec / 60, holdSec % 60), "HOLD")
            } else {
                StatPair("${metrics.repCount}", "REPS")
                StatPair("%.1f".format(metrics.tempoRpm), "RPM")
            }
            StatPair("${metrics.rhythmConsistency}%", "RHYTHM")
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.extendedColors.hairline),
    )
}

@Composable
private fun StatPair(value: String, label: String, valueColor: Color = Color.Unspecified) {
    Column {
        Text(
            text = value,
            style = IOMotionTextStyles.sessionStatValue,
            color = if (valueColor == Color.Unspecified) MaterialTheme.colorScheme.onBackground else valueColor,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = IOMotionTextStyles.sessionStatLabel,
            color = MaterialTheme.extendedColors.textMuted,
        )
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "No sessions yet",
                style = IOMotionTextStyles.sessionRowName,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Complete a live or video session to see it here.",
                style = IOMotionTextStyles.metaTimestamp,
                color = MaterialTheme.extendedColors.textMuted,
            )
        }
    }
}
