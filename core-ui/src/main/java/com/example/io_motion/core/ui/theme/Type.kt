@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package com.example.io_motion.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.io_motion.core.ui.R

/**
 * Both families ship as single variable fonts (wght axis, Bricolage also has opsz/wdth). Each
 * weight below is a distinct named instance of the same underlying file — Android instantiates it
 * per [FontVariation.Settings] on API 26+; pre-26 devices fall back to the font's default instance
 * for all weights (a graceful, non-crashing degradation).
 */
val BricolageGrotesque = FontFamily(
    Font(R.font.bricolage_grotesque_variable, weight = FontWeight.W700, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
    Font(R.font.bricolage_grotesque_variable, weight = FontWeight.W800, variationSettings = FontVariation.Settings(FontVariation.weight(800))),
)

val Inter = FontFamily(
    Font(R.font.inter_variable, weight = FontWeight.W400, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.inter_variable, weight = FontWeight.W600, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.inter_variable, weight = FontWeight.W700, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
    Font(R.font.inter_variable, weight = FontWeight.W800, variationSettings = FontVariation.Settings(FontVariation.weight(800))),
)

// Set of Material typography styles to start with
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
    /* Other default text styles to override
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
    */
)

/**
 * Named text styles for the redesigned Home/Details/History screens (doc/CLAUDE_CODE_PROMPT_DESIGN.md).
 * Kept separate from the M3 [Typography] slots above, whose sizes don't match this design's scale.
 */
object IOMotionTextStyles {
    val wordmark = TextStyle(fontFamily = BricolageGrotesque, fontWeight = FontWeight.W800, fontSize = 36.sp, letterSpacing = (-1.2).sp)
    val subtitle = TextStyle(fontFamily = Inter, fontWeight = FontWeight.W700, fontSize = 11.sp, letterSpacing = 2.sp)

    val sectionLabel = TextStyle(fontFamily = Inter, fontWeight = FontWeight.W700, fontSize = 12.sp, letterSpacing = 1.6.sp)

    val exerciseRowName = TextStyle(fontFamily = BricolageGrotesque, fontWeight = FontWeight.W700, fontSize = 21.sp)

    val segmentedLabel = TextStyle(fontFamily = Inter, fontWeight = FontWeight.W700, fontSize = 13.sp)
    val modelVariantCaption = TextStyle(fontFamily = Inter, fontWeight = FontWeight.W400, fontSize = 12.sp)

    val ctaLabel = TextStyle(fontFamily = Inter, fontWeight = FontWeight.W800, fontSize = 15.sp, letterSpacing = 1.sp)

    val screenTitle = TextStyle(fontFamily = BricolageGrotesque, fontWeight = FontWeight.W800, fontSize = 26.sp)
    val historyTitle = TextStyle(fontFamily = BricolageGrotesque, fontWeight = FontWeight.W800, fontSize = 24.sp)

    val metaTimestamp = TextStyle(fontFamily = Inter, fontWeight = FontWeight.W400, fontSize = 12.sp)
    val metaModeVariant = TextStyle(fontFamily = Inter, fontWeight = FontWeight.W700, fontSize = 12.sp, letterSpacing = 1.sp)

    val scoreCaption = TextStyle(fontFamily = Inter, fontWeight = FontWeight.W700, fontSize = 11.sp, letterSpacing = 2.sp)
    val scoreNumber = TextStyle(fontFamily = BricolageGrotesque, fontWeight = FontWeight.W800, fontSize = 58.sp)

    val statValue = TextStyle(fontFamily = BricolageGrotesque, fontWeight = FontWeight.W700, fontSize = 24.sp)
    val statLabel = TextStyle(fontFamily = Inter, fontWeight = FontWeight.W700, fontSize = 10.sp, letterSpacing = 1.2.sp)

    val repTag = TextStyle(fontFamily = BricolageGrotesque, fontWeight = FontWeight.W700, fontSize = 15.sp)
    val repAngleRange = TextStyle(fontFamily = Inter, fontWeight = FontWeight.W600, fontSize = 16.sp)
    val repMeta = TextStyle(fontFamily = Inter, fontWeight = FontWeight.W400, fontSize = 12.sp)
    val repScore = TextStyle(fontFamily = BricolageGrotesque, fontWeight = FontWeight.W800, fontSize = 20.sp)

    val sessionRowName = TextStyle(fontFamily = BricolageGrotesque, fontWeight = FontWeight.W700, fontSize = 19.sp)
    val sessionRowMeta = TextStyle(fontFamily = Inter, fontWeight = FontWeight.W700, fontSize = 11.sp, letterSpacing = 1.sp)
    val sessionStatValue = TextStyle(fontFamily = BricolageGrotesque, fontWeight = FontWeight.W800, fontSize = 22.sp)
    val sessionStatLabel = TextStyle(fontFamily = Inter, fontWeight = FontWeight.W700, fontSize = 10.sp, letterSpacing = 1.sp)
}
