package com.example.io_motion.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.io_motion.core.analysis.model.RepMetrics
import com.example.io_motion.core.analysis.model.SessionMetrics
import com.example.io_motion.core.ui.theme.extendedColors
import kotlin.math.roundToInt

/**
 * Session-report metric cards shared by the video-analysis result screen and the history
 * report screen — previously copy-pasted identically (~150 lines) between the two.
 */

@Composable
fun BigMetricCard(value: String, label: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
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
fun RepMetricsGrid(metrics: SessionMetrics, modifier: Modifier = Modifier) {
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

/**
 * @param showQualityCard Whether to include a redundant "QUALITY" card alongside "DURATION" in
 *   the second row. The history report screen shows it (its historical behavior, preserved
 *   here); the video-result screen omits it since a `MetricGauge` already displays the same
 *   session quality score above this grid.
 */
@Composable
fun PlankMetricsGrid(metrics: SessionMetrics, modifier: Modifier = Modifier, showQualityCard: Boolean = false) {
    val holdSec = metrics.validHoldMs / 1_000L
    val durationSec = metrics.totalDurationMs / 1_000L
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BigMetricCard("%d:%02d".format(holdSec / 60, holdSec % 60), "VALID HOLD", Modifier.weight(1f))
            BigMetricCard("%.1f°".format(metrics.avgBodyLineAngle), "BODY LINE", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BigMetricCard("%d:%02d".format(durationSec / 60, durationSec % 60), "DURATION", Modifier.weight(1f))
            if (showQualityCard) {
                BigMetricCard("${metrics.sessionQualityScore}", "QUALITY", Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun RepCard(repNumber: Int, rep: RepMetrics, modifier: Modifier = Modifier) {
    val qualityColor = when {
        rep.qualityScore >= 80 -> MaterialTheme.extendedColors.success
        rep.qualityScore >= 60 -> MaterialTheme.extendedColors.warning
        else -> MaterialTheme.colorScheme.error
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
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
                shape = MaterialTheme.shapes.extraSmall,
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
