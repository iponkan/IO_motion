package com.example.io_motion.core.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * The design's "cut corner" motif: every filled/interactive surface has its top-left corner cut
 * at a 45° diagonal, all other corners square, instead of rounded corners.
 */
class CutCornerTopLeft(private val cut: Dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val cutPx = with(density) { cut.toPx() }
        return Outline.Generic(Path().apply {
            moveTo(cutPx, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        })
    }
}

/** Global toggle for the cut-corner motif — set false to render plain rectangles instead. */
val LocalCutCornerEnabled = compositionLocalOf { false }

/** Cut sizes used across the redesigned screens. */
object CutCorner {
    val ctaButton = 16.dp
    val selectedRow = 16.dp
    val segmentedIndicator = 10.dp
}

fun cutCornerShape(cut: Dp, enabled: Boolean): Shape =
    if (enabled) CutCornerTopLeft(cut) else RectangleShape
