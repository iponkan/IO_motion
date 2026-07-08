package com.example.io_motion.data.repository

import android.util.Log
import com.example.io_motion.core.common.models.MealType
import com.example.io_motion.data.dao.DietDao
import com.example.io_motion.data.di.ApplicationScope
import com.example.io_motion.data.entity.DailyLogEntity
import com.example.io_motion.data.entity.DietEntryEntity
import com.example.io_motion.data.model.DayLog
import com.example.io_motion.data.model.DietEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "DietRepository"

class DietRepositoryImpl @Inject constructor(
    private val dao: DietDao,
    @ApplicationScope private val appScope: CoroutineScope,
) : DietRepository {

    override fun dayLog(localDate: String): Flow<DayLog> =
        combine(
            dao.observeEntries(localDate),
            dao.observeDailyLog(localDate),
        ) { entries, dailyLog ->
            DayLog(
                localDate = localDate,
                meals = entries
                    .mapNotNull { it.toEntryOrNull() }
                    .groupBy { it.mealType },
                waterCups = dailyLog?.waterCups ?: 0,
            )
        }

    override fun addEntry(localDate: String, mealType: MealType, name: String, kcal: Int) {
        appScope.launch {
            dao.insertEntry(
                DietEntryEntity(
                    localDate = localDate,
                    mealType = mealType.name,
                    name = name,
                    kcal = kcal,
                    loggedAt = System.currentTimeMillis(),
                )
            )
        }
    }

    override fun removeEntry(id: Long) {
        appScope.launch { dao.deleteEntry(id) }
    }

    override fun setWater(localDate: String, cups: Int) {
        appScope.launch { dao.upsertDailyLog(DailyLogEntity(localDate = localDate, waterCups = cups)) }
    }
}

// ── Mappers ──────────────────────────────────────────────────────────────────

/** Drops (rather than throwing on) a row whose stored [DietEntryEntity.mealType] is unknown. */
private fun DietEntryEntity.toEntryOrNull(): DietEntry? {
    val type = runCatching { MealType.valueOf(mealType) }.getOrNull()
    if (type == null) {
        Log.w(TAG, "Dropping diet entry $id: unknown mealType '$mealType'")
        return null
    }
    return DietEntry(id = id, mealType = type, name = name, kcal = kcal, loggedAt = loggedAt)
}
