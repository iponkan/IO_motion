package com.example.io_motion.feature.history

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.io_motion.core.analysis.model.RepMetrics
import com.example.io_motion.core.analysis.model.SessionMetrics
import com.example.io_motion.core.common.models.AnalysisMode
import com.example.io_motion.core.common.models.ExerciseType
import com.example.io_motion.core.ui.components.MetricGauge
import com.example.io_motion.data.model.SessionRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun SessionReportScreen(
    sessionId: Long,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SessionReportViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(sessionId) { viewModel.load(sessionId) }

    LaunchedEffect(Unit) {
        viewModel.shareEvents.collect { content ->
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = content.mimeType
                putExtra(Intent.EXTRA_TEXT, content.text)
            }
            context.startActivity(Intent.createChooser(intent, "Share export"))
        }
    }

    Surface(modifier = modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            uiState.record == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Session not found.")
            }
            else -> ReportContent(
                record = uiState.record!!,
                onNavigateBack = onNavigateBack,
                onExportJson = viewModel::exportJson,
                onExportCsv = viewModel::exportCsv,
            )
        }
    }
}

@Composable
private fun ReportContent(
    record: SessionRecord,
    onNavigateBack: () -> Unit,
    onExportJson: () -> Unit,
    onExportCsv: () -> Unit,
) {
    val metrics = record.metrics
    val isPlank = metrics.exerciseType == ExerciseType.PLANK

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 16.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onNavigateBack) {
                    Text("← Back", style = MaterialTheme.typography.labelLarge)
                }
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = metrics.exerciseType.displayName(),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    )
                    Text(
                        text = record.recordedAt.toDateString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    )
                }
            }
        }

        // ── Metadata badges ───────────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SmallBadge(if (record.analysisMode == AnalysisMode.LIVE) "Live" else "Video")
                SmallBadge(record.modelVariant.lowercase().replaceFirstChar { it.uppercase() })
            }
        }

        // ── Quality gauge ─────────────────────────────────────────────────────
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                MetricGauge(value = metrics.sessionQualityScore, label = "SESSION QUALITY")
            }
        }

        // ── Metrics grid ──────────────────────────────────────────────────────
        item {
            Spacer(Modifier.height(16.dp))
            if (isPlank) PlankMetricsGrid(metrics, Modifier.padding(horizontal = 16.dp))
            else RepMetricsGrid(metrics, Modifier.padding(horizontal = 16.dp))
        }

        // ── Per-rep breakdown ─────────────────────────────────────────────────
        if (!isPlank && metrics.reps.isNotEmpty()) {
            item {
                Spacer(Modifier.height(24.dp))
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Per-Rep Breakdown",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                Spacer(Modifier.height(8.dp))
            }
            itemsIndexed(metrics.reps) { index, rep ->
                RepCard(
                    repNumber = index + 1,
                    rep = rep,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        }

        // ── Export buttons ────────────────────────────────────────────────────
        item {
            Spacer(Modifier.height(28.dp))
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onExportCsv,
                    modifier = Modifier.weight(1f).height(48.dp),
                ) { Text("Export CSV") }
                Button(
                    onClick = onExportJson,
                    modifier = Modifier.weight(1f).height(48.dp),
                ) { Text("Export JSON") }
            }
        }
    }
}

// ── Metrics grids ─────────────────────────────────────────────────────────────

@Composable
private fun RepMetricsGrid(metrics: SessionMetrics, modifier: Modifier = Modifier) {
    val durationSec = metrics.totalDurationMs / 1_000L
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BigMetricCard("${metrics.repCount}", "REPS", Modifier.weight(1f))
            BigMetricCard("${metrics.rejectedRepCount}", "REJECTED", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BigMetricCard("%.1f".format(metrics.tempoRpm), "RPM", Modifier.weight(1f))
            BigMetricCard("%.0f°".format(metrics.avgRomDegrees), "AVG ROM", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BigMetricCard("%d:%02d".format(durationSec / 60, durationSec % 60), "DURATION", Modifier.weight(1f))
            BigMetricCard("${metrics.rhythmConsistency}%", "RHYTHM", Modifier.weight(1f))
        }
    }
}

@Composable
private fun PlankMetricsGrid(metrics: SessionMetrics, modifier: Modifier = Modifier) {
    val holdSec = metrics.validHoldMs / 1_000L
    val durationSec = metrics.totalDurationMs / 1_000L
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BigMetricCard("%d:%02d".format(holdSec / 60, holdSec % 60), "VALID HOLD", Modifier.weight(1f))
            BigMetricCard("%.1f°".format(metrics.avgBodyLineAngle), "BODY LINE", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BigMetricCard("%d:%02d".format(durationSec / 60, durationSec % 60), "DURATION", Modifier.weight(1f))
            BigMetricCard("${metrics.sessionQualityScore}", "QUALITY", Modifier.weight(1f))
        }
    }
}

// ── Shared composables ─────────────────────────────────────────────────────────

@Composable
private fun BigMetricCard(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
            )
        }
    }
}

@Composable
private fun RepCard(repNumber: Int, rep: RepMetrics, modifier: Modifier = Modifier) {
    val qualityColor = when {
        rep.qualityScore >= 80 -> Color(0xFF2E7D32)
        rep.qualityScore >= 60 -> Color(0xFFF57F17)
        else -> MaterialTheme.colorScheme.error
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "#$repNumber",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.width(36.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${rep.minAngle.roundToInt()}° → ${rep.maxAngle.roundToInt()}°",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                )
                Text(
                    text = "${rep.rom.roundToInt()}° ROM · ${rep.durationMs}ms",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            }
            Surface(
                color = qualityColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(6.dp),
            ) {
                Text(
                    text = "${rep.qualityScore}",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = qualityColor,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun SmallBadge(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(6.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

private fun ExerciseType.displayName() = when (this) {
    ExerciseType.SQUAT   -> "Squat"
    ExerciseType.PUSH_UP -> "Push-up"
    ExerciseType.SIT_UP  -> "Sit-up"
    ExerciseType.PLANK   -> "Plank"
}

private fun Long.toDateString(): String =
    SimpleDateFormat("MMM d, yyyy  HH:mm", Locale.getDefault()).format(Date(this))
