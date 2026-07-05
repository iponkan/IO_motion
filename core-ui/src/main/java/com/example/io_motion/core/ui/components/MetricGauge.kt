package com.example.io_motion.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.io_motion.core.ui.theme.extendedColors

/**
 * A 270° arc gauge that displays a 0–100 score. The arc color transitions from red (low)
 * through amber (mid) to green (high) to give an instant visual quality signal.
 *
 * [contentColor] defaults to the theme's `onSurface` so this reads correctly on a plain themed
 * background (e.g. the session report). Callers rendering it over a fixed dark scrim (the live
 * camera overlay) should pass an explicit `Color.White` instead.
 */
@Composable
fun MetricGauge(
    value: Int,
    label: String,
    modifier: Modifier = Modifier,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    val fraction = value.coerceIn(0, 100) / 100f
    val arcColor = when {
        fraction >= 0.75f -> MaterialTheme.extendedColors.success
        fraction >= 0.50f -> MaterialTheme.extendedColors.warning
        else              -> MaterialTheme.colorScheme.error
    }

    Box(
        modifier = modifier.size(88.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(88.dp)) {
            val strokeWidth = 8.dp.toPx()
            val inset = strokeWidth / 2f
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            val topLeft = Offset(inset, inset)

            drawArc(
                color = contentColor.copy(alpha = 0.2f),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
            if (fraction > 0f) {
                drawArc(
                    color = arcColor,
                    startAngle = 135f,
                    sweepAngle = 270f * fraction,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "$value", style = MaterialTheme.typography.titleLarge, color = contentColor)
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = contentColor.copy(alpha = 0.7f))
        }
    }
}
