package com.example.io_motion.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.io_motion.core.ui.theme.CutCorner
import com.example.io_motion.core.ui.theme.IOMotionTextStyles
import com.example.io_motion.core.ui.theme.LocalCutCornerEnabled
import com.example.io_motion.core.ui.theme.cutCornerShape
import com.example.io_motion.core.ui.theme.extendedColors

/**
 * Section header used above pickers on Home/Settings: a small accent square + uppercase muted
 * label.
 */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(5.dp).background(MaterialTheme.colorScheme.primary))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, style = IOMotionTextStyles.sectionLabel, color = MaterialTheme.extendedColors.textMuted)
    }
}

/**
 * A hairline-bordered track with a sliding accent-filled indicator behind the active option —
 * used for Analysis Mode / Model Variant on Home and the Theme/Model Variant pickers on Settings.
 */
@Composable
fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cutCornerEnabled = LocalCutCornerEnabled.current
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
                .background(MaterialTheme.colorScheme.primary, cutCornerShape(CutCorner.segmentedIndicator, cutCornerEnabled)),
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
