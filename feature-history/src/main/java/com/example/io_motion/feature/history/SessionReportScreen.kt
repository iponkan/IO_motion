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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.io_motion.core.common.models.AnalysisMode
import com.example.io_motion.core.common.models.ExerciseType
import com.example.io_motion.core.common.models.displayName
import com.example.io_motion.core.ui.components.MetricGauge
import com.example.io_motion.core.ui.components.PlankMetricsGrid
import com.example.io_motion.core.ui.components.RepCard
import com.example.io_motion.core.ui.components.RepMetricsGrid
import com.example.io_motion.data.model.SessionRecord

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
                putExtra(Intent.EXTRA_STREAM, content.uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
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
            if (isPlank) {
                PlankMetricsGrid(metrics, Modifier.padding(horizontal = 16.dp), showQualityCard = true)
            } else {
                RepMetricsGrid(metrics, Modifier.padding(horizontal = 16.dp))
            }
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

// ── Shared composables ─────────────────────────────────────────────────────────

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
