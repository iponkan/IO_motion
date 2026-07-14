package com.example.io_motion.feature.diet

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Pure, JVM-testable math for the Diet Planning screens: calorie-ring fraction, water clamping, and
 * the per-day date key. Kept free of Android/Compose so it can be exhaustively unit-tested; the
 * kcal totals themselves live on `DayLog` (`:data`).
 */
object DietMath {

    /** Water may slightly overfill past the target of 8 (design §6); the stepper clamps here. */
    const val WATER_MAX_CUPS = 12

    /**
     * Calorie-ring fill fraction in `0f..1f`. Clamps at a full circle so the arc never overdraws
     * past 100% (design §6), and treats a non-positive target as an empty ring.
     */
    fun ringFraction(totalKcal: Int, targetKcal: Int): Float {
        if (targetKcal <= 0) return 0f
        return (totalKcal.toFloat() / targetKcal).coerceIn(0f, 1f)
    }

    /** Clamps a water-cup count to `0..`[WATER_MAX_CUPS]. */
    fun clampWater(cups: Int): Int = cups.coerceIn(0, WATER_MAX_CUPS)

    /**
     * ISO `yyyy-MM-dd` key identifying "today" in [zoneId]. All diet state is keyed by this, so the
     * day naturally rolls over at local midnight (past days are stored but not browsable — a known
     * limitation for this iteration).
     */
    fun dateKey(nowEpochMillis: Long, zoneId: ZoneId): String =
        LocalDate.ofInstant(Instant.ofEpochMilli(nowEpochMillis), zoneId).toString()
}
