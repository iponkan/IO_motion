package com.example.io_motion.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Design tokens from doc/CLAUDE_CODE_PROMPT_DESIGN.md that don't map onto a standard M3
 * colorScheme role (muted text tiers, hairline dividers, the score-color trio).
 */
data class ExtendedColors(
    val textMuted: Color,
    val textMutedSecondary: Color,
    val hairline: Color,
    val segmentedTrackBorder: Color,
    val accentOn: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
)

val LightExtendedColors = ExtendedColors(
    textMuted = TextMutedLight,
    textMutedSecondary = TextMutedSecondaryLight,
    hairline = HairlineLight.copy(alpha = HairlineLightAlpha),
    segmentedTrackBorder = SegmentedTrackBorderLight.copy(alpha = SegmentedTrackBorderLightAlpha),
    accentOn = AccentOn,
    success = SuccessLight,
    warning = WarningLight,
    danger = DangerLight,
)

val DarkExtendedColors = ExtendedColors(
    textMuted = TextMutedDark,
    textMutedSecondary = TextMutedSecondaryDark,
    hairline = HairlineDark.copy(alpha = HairlineDarkAlpha),
    segmentedTrackBorder = SegmentedTrackBorderDark.copy(alpha = SegmentedTrackBorderDarkAlpha),
    accentOn = AccentOn,
    success = SuccessDark,
    warning = WarningDark,
    danger = DangerDark,
)

val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }

val MaterialTheme.extendedColors: ExtendedColors
    @Composable get() = LocalExtendedColors.current

/** Score-color rule: score >= 85 -> success; 60-84 -> warning; below 60 -> danger. */
fun ExtendedColors.scoreColor(score: Int): Color = when {
    score >= 85 -> success
    score >= 60 -> warning
    else        -> danger
}
