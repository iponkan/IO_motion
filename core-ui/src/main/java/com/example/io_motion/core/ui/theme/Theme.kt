package com.example.io_motion.core.ui.theme

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.example.io_motion.core.common.models.ThemeMode

// Muted, opaque tint tones derived from each theme's background — used for M3 "container" roles
// (badges, secondary cards) that the design doc doesn't define tokens for but older screens
// (LiveScreen/VideoScreen, outside this redesign's scope) still rely on for visual consistency.
private fun containerTone(background: Color, tint: Color, fraction: Float = 0.18f): Color =
    lerp(background, tint, fraction)

private val LightColorScheme = lightColorScheme(
    primary = Accent,
    onPrimary = AccentOn,
    primaryContainer = containerTone(BackgroundLight, Accent),
    onPrimaryContainer = TextPrimaryLight,
    secondary = TextMutedLight,
    onSecondary = BackgroundLight,
    secondaryContainer = containerTone(BackgroundLight, TextMutedLight),
    onSecondaryContainer = TextPrimaryLight,
    tertiary = SuccessLight,
    onTertiary = BackgroundLight,
    tertiaryContainer = containerTone(BackgroundLight, SuccessLight),
    onTertiaryContainer = TextPrimaryLight,
    background = BackgroundLight,
    onBackground = TextPrimaryLight,
    surface = BackgroundLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = containerTone(BackgroundLight, TextMutedLight),
    onSurfaceVariant = TextMutedLight,
    surfaceContainerLow = containerTone(BackgroundLight, TextMutedLight, 0.10f),
    outline = TextMutedSecondaryLight,
    error = DangerLight,
    onError = AccentOn,
    errorContainer = containerTone(BackgroundLight, DangerLight),
    onErrorContainer = TextPrimaryLight,
)

private val DarkColorScheme = darkColorScheme(
    primary = Accent,
    onPrimary = AccentOn,
    primaryContainer = containerTone(BackgroundDark, Accent),
    onPrimaryContainer = TextPrimaryDark,
    secondary = TextMutedDark,
    onSecondary = BackgroundDark,
    secondaryContainer = containerTone(BackgroundDark, TextMutedDark),
    onSecondaryContainer = TextPrimaryDark,
    tertiary = SuccessDark,
    onTertiary = BackgroundDark,
    tertiaryContainer = containerTone(BackgroundDark, SuccessDark),
    onTertiaryContainer = TextPrimaryDark,
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = BackgroundDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = containerTone(BackgroundDark, TextMutedDark),
    onSurfaceVariant = TextMutedDark,
    surfaceContainerLow = containerTone(BackgroundDark, TextMutedDark, 0.10f),
    outline = TextMutedSecondaryDark,
    error = DangerDark,
    onError = AccentOn,
    errorContainer = containerTone(BackgroundDark, DangerDark),
    onErrorContainer = TextPrimaryDark,
)

val IOMotionShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun IO_motionTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit,
) {
    val darkTheme = themeMode == ThemeMode.DARK
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = IOMotionShapes,
        ) {
            // Guarantees every screen paints over the full window with the resolved theme
            // background, regardless of what the Activity's static XML theme happens to show.
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                content()
            }
        }
    }
}
