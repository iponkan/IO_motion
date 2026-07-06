package com.example.io_motion.feature.history

import android.content.Intent
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.io_motion.core.common.models.ExerciseType
import com.example.io_motion.core.common.models.displayName
import com.example.io_motion.core.common.models.metaLabel
import com.example.io_motion.core.ui.theme.CutCorner
import com.example.io_motion.core.ui.theme.IOMotionTextStyles
import com.example.io_motion.core.ui.theme.LocalCutCornerEnabled
import com.example.io_motion.core.ui.theme.cutCornerShape
import com.example.io_motion.core.ui.theme.extendedColors
import com.example.io_motion.core.ui.theme.scoreColor
import com.example.io_motion.data.model.SessionRecord
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
                putExtra(Intent.EXTRA_STREAM, content.uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share export"))
        }
    }

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
                text = uiState.record?.metrics?.exerciseType?.displayName() ?: "Session Report",
                style = IOMotionTextStyles.screenTitle,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            uiState.record == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Session not found.", color = MaterialTheme.extendedColors.textMuted)
            }
            else -> ReportContent(
                record = uiState.record!!,
                onExportJson = viewModel::exportJson,
                onExportCsv = viewModel::exportCsv,
            )
        }
    }
}

@Composable
private fun ReportContent(
    record: SessionRecord,
    onExportJson: () -> Unit,
    onExportCsv: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val metrics = record.metrics
    val isPlank = metrics.exerciseType == ExerciseType.PLANK
    val cutCornerEnabled = LocalCutCornerEnabled.current

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 0.dp),
    ) {
        // ── Metadata ──────────────────────────────────────────────────────────
        item {
            Column(modifier = Modifier.padding(start = 36.dp)) {
                Text(
                    text = record.recordedAt.toDateString(),
                    style = IOMotionTextStyles.metaTimestamp,
                    color = MaterialTheme.extendedColors.textMuted,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${record.analysisMode.metaLabel()} · ${record.modelVariant.uppercase()}",
                    style = IOMotionTextStyles.metaModeVariant,
                    color = MaterialTheme.extendedColors.textMutedSecondary,
                )
            }
        }

        // ── Score ring ────────────────────────────────────────────────────────
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "SESSION QUALITY",
                    style = IOMotionTextStyles.scoreCaption,
                    color = MaterialTheme.extendedColors.textMuted,
                )
                Spacer(modifier = Modifier.height(16.dp))
                ScoreRing(score = metrics.sessionQualityScore)
            }
        }

        // ── Stat grid ─────────────────────────────────────────────────────────
        item {
            Spacer(Modifier.height(28.dp))
            val cells = if (isPlank) {
                val holdSec = metrics.validHoldMs / 1_000L
                val durationSec = metrics.totalDurationMs / 1_000L
                listOf(
                    "%d:%02d".format(holdSec / 60, holdSec % 60) to "VALID HOLD",
                    "%.1f°".format(metrics.avgBodyLineAngle) to "BODY LINE",
                    "%d:%02d".format(durationSec / 60, durationSec % 60) to "DURATION",
                )
            } else {
                val durationSec = metrics.totalDurationMs / 1_000L
                listOf(
                    "${metrics.repCount}" to "REPS",
                    "${metrics.rejectedRepCount}" to "REJECTED",
                    "%.1f".format(metrics.tempoRpm) to "RPM",
                    "%.0f°".format(metrics.avgRomDegrees) to "AVG ROM",
                    "%d:%02d".format(durationSec / 60, durationSec % 60) to "DURATION",
                    "${metrics.rhythmConsistency}%" to "RHYTHM",
                )
            }
            HairlineStatGrid(cells = cells, columns = 3)
        }

        // ── Per-rep breakdown ─────────────────────────────────────────────────
        if (!isPlank && metrics.reps.isNotEmpty()) {
            item {
                Spacer(Modifier.height(32.dp))
                Text(
                    text = "PER-REP BREAKDOWN",
                    style = IOMotionTextStyles.sectionLabel,
                    color = MaterialTheme.extendedColors.textMuted,
                )
                Spacer(Modifier.height(14.dp))
            }
            itemsIndexed(metrics.reps) { index, rep ->
                RepRow(
                    repNumber = index + 1,
                    minAngle = rep.minAngle,
                    maxAngle = rep.maxAngle,
                    rom = rep.rom,
                    durationMs = rep.durationMs,
                    qualityScore = rep.qualityScore,
                    showDivider = index != metrics.reps.lastIndex,
                )
            }
        }

        // ── Export buttons ────────────────────────────────────────────────────
        item {
            Spacer(Modifier.height(32.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ExportButton("EXPORT CSV", onExportCsv, Modifier.weight(1f), cutCornerEnabled)
                ExportButton("EXPORT JSON", onExportJson, Modifier.weight(1f), cutCornerEnabled)
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun ScoreRing(score: Int, modifier: Modifier = Modifier) {
    val fraction = score.coerceIn(0, 100) / 100f
    val trackColor = MaterialTheme.extendedColors.hairline
    val arcColor = MaterialTheme.extendedColors.scoreColor(score)

    Box(modifier = modifier.size(172.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 8.dp.toPx()
            val inset = strokeWidth / 2f
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            val topLeft = Offset(inset, inset)

            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
            if (fraction > 0f) {
                drawArc(
                    color = arcColor,
                    startAngle = -90f,
                    sweepAngle = 360f * fraction,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
            }
        }
        Text(
            text = "$score",
            style = IOMotionTextStyles.scoreNumber,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun HairlineStatGrid(cells: List<Pair<String, String>>, columns: Int, modifier: Modifier = Modifier) {
    val hairline = MaterialTheme.extendedColors.hairline
    val rows = cells.chunked(columns)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                val strokeWidth = 1.dp.toPx()
                drawLine(hairline, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth)
                drawLine(hairline, Offset(0f, 0f), Offset(0f, size.height), strokeWidth)
            },
    ) {
        rows.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                row.forEach { (value, label) ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .drawBehind {
                                val strokeWidth = 1.dp.toPx()
                                drawLine(hairline, Offset(size.width, 0f), Offset(size.width, size.height), strokeWidth)
                                drawLine(hairline, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth)
                            }
                            .padding(vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(text = value, style = IOMotionTextStyles.statValue, color = MaterialTheme.colorScheme.onBackground)
                        Spacer(Modifier.height(4.dp))
                        Text(text = label, style = IOMotionTextStyles.statLabel, color = MaterialTheme.extendedColors.textMuted)
                    }
                }
                repeat(columns - row.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun RepRow(
    repNumber: Int,
    minAngle: Double,
    maxAngle: Double,
    rom: Double,
    durationMs: Long,
    qualityScore: Int,
    showDivider: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "#$repNumber",
                style = IOMotionTextStyles.repTag,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.width(36.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${minAngle.roundToInt()}° → ${maxAngle.roundToInt()}°",
                    style = IOMotionTextStyles.repAngleRange,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${rom.roundToInt()}° ROM · ${durationMs}ms",
                    style = IOMotionTextStyles.repMeta,
                    color = MaterialTheme.extendedColors.textMuted,
                )
            }
            Text(
                text = "$qualityScore",
                style = IOMotionTextStyles.repScore,
                color = MaterialTheme.extendedColors.scoreColor(qualityScore),
            )
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
private fun ExportButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, cutCornerEnabled: Boolean = true) {
    Box(
        modifier = modifier
            .height(48.dp)
            .background(
                color = MaterialTheme.extendedColors.hairline,
                shape = cutCornerShape(CutCorner.selectedRow, cutCornerEnabled),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = IOMotionTextStyles.segmentedLabel,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}
