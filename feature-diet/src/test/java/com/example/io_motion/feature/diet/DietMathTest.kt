package com.example.io_motion.feature.diet

import com.example.io_motion.core.common.models.MealType
import com.example.io_motion.data.model.DayLog
import com.example.io_motion.data.model.DietEntry
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class DietMathTest {

    // ── ringFraction ───────────────────────────────────────────────────────────────

    @Test
    fun `ring fraction is total over target below the cap`() {
        assertEquals(0.5f, DietMath.ringFraction(1100, 2200), 1e-6f)
    }

    @Test
    fun `ring fraction clamps at one and never overdraws`() {
        assertEquals(1f, DietMath.ringFraction(3000, 2200), 1e-6f)
    }

    @Test
    fun `ring fraction is zero for empty log`() {
        assertEquals(0f, DietMath.ringFraction(0, 2200), 1e-6f)
    }

    @Test
    fun `ring fraction is zero for a non-positive target`() {
        assertEquals(0f, DietMath.ringFraction(500, 0), 1e-6f)
    }

    // ── clampWater ─────────────────────────────────────────────────────────────────

    @Test
    fun `water clamps to zero floor`() {
        assertEquals(0, DietMath.clampWater(-1))
    }

    @Test
    fun `water clamps to the max ceiling`() {
        assertEquals(DietMath.WATER_MAX_CUPS, DietMath.clampWater(DietMath.WATER_MAX_CUPS + 3))
    }

    @Test
    fun `water passes through inside the range`() {
        assertEquals(4, DietMath.clampWater(4))
    }

    // ── dateKey ────────────────────────────────────────────────────────────────────

    @Test
    fun `date key is ISO yyyy-MM-dd in the given zone`() {
        val epoch = Instant.parse("2026-07-09T10:00:00Z").toEpochMilli()
        assertEquals("2026-07-09", DietMath.dateKey(epoch, ZoneId.of("UTC")))
    }

    @Test
    fun `date key respects the zone at a midnight boundary`() {
        // 02:30 UTC is still the previous day in New York (UTC-04:00 in July).
        val epoch = Instant.parse("2026-07-09T02:30:00Z").toEpochMilli()
        assertEquals("2026-07-08", DietMath.dateKey(epoch, ZoneId.of("America/New_York")))
    }

    // ── kcal totals (DayLog, :data) ─────────────────────────────────────────────────

    private fun entry(type: MealType, kcal: Int) =
        DietEntry(id = 0, mealType = type, name = "x", kcal = kcal, loggedAt = 0)

    @Test
    fun `day total sums kcal across all meals and per-meal subtotal is scoped`() {
        val log = DayLog(
            localDate = "2026-07-09",
            meals = mapOf(
                MealType.BREAKFAST to listOf(entry(MealType.BREAKFAST, 310)),
                MealType.LUNCH to listOf(entry(MealType.LUNCH, 420), entry(MealType.LUNCH, 80)),
                MealType.SNACKS to listOf(entry(MealType.SNACKS, 150)),
            ),
            waterCups = 4,
        )
        assertEquals(960, log.totalKcal)
        assertEquals(500, log.kcalFor(MealType.LUNCH))
        assertEquals(0, log.kcalFor(MealType.DINNER))
    }
}
