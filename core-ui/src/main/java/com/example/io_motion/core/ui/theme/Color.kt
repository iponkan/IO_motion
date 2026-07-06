package com.example.io_motion.core.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.io_motion.core.common.models.AccentTheme

// ── Design tokens — dark theme (doc/CLAUDE_CODE_PROMPT_DESIGN.md) ──────────────
val BackgroundDark = Color(0xFF141109)
val TextPrimaryDark = Color(0xFFF6EFE3)
val TextMutedDark = Color(0xFF9C9284)
val TextMutedSecondaryDark = Color(0xFF7D766A)
val HairlineDark = Color(0xFFF6EFE3)
const val HairlineDarkAlpha = 0.14f
val SegmentedTrackBorderDark = Color(0xFFF6EFE3)
const val SegmentedTrackBorderDarkAlpha = 0.18f

// ── Design tokens — light theme ─────────────────────────────────────────────────
val BackgroundLight = Color(0xFFFBF6ED)
val TextPrimaryLight = Color(0xFF1B1610)
val TextMutedLight = Color(0xFF8A8072)
val TextMutedSecondaryLight = Color(0xFFA69D8F)
val HairlineLight = Color(0xFF1B1610)
const val HairlineLightAlpha = 0.12f
val SegmentedTrackBorderLight = Color(0xFF1B1610)
const val SegmentedTrackBorderLightAlpha = 0.16f

// ── Accent — user-selectable, same across both themes ───────────────────────────
val AccentBlue = Color(0xFF2F6BFF)
val AccentOrange = Color(0xFFFF5A1F)
val AccentLime = Color(0xFFC6FF3D)

// "Accent-on" contrast rule: luminance = (0.299r + 0.587g + 0.114b)/255; >0.62 -> dark ink, else light.
val LightOnAccent = Color(0xFFFBF6ED)
val DarkInkOnAccent = Color(0xFF161109)

fun AccentTheme.toColor(): Color = when (this) {
    AccentTheme.BLUE   -> AccentBlue
    AccentTheme.ORANGE -> AccentOrange
    AccentTheme.LIME   -> AccentLime
}

/** Accent-on contrast rule: luminance = (0.299r + 0.587g + 0.114b)/255; >0.62 -> dark ink, else light. */
fun accentOnColorFor(accent: Color): Color {
    val luminance = (0.299f * accent.red * 255f + 0.587f * accent.green * 255f + 0.114f * accent.blue * 255f) / 255f
    return if (luminance > 0.62f) DarkInkOnAccent else LightOnAccent
}

// ── Score-color rule (ring, per-rep scores, history quality numbers) ────────────
val SuccessDark = Color(0xFF5EE38A)
val WarningDark = Color(0xFFFFC24B)
val DangerDark = Color(0xFFFF7A6B)

val SuccessLight = Color(0xFF1E8E4F)
val WarningLight = Color(0xFFB4740A)
val DangerLight = Color(0xFFC43A2E)
