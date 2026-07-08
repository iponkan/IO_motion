package com.example.io_motion.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Per-day log keyed by ISO local date. Water lives here (per-day data, like meals) not DataStore. */
@Entity(tableName = "daily_log_entities")
data class DailyLogEntity(
    @PrimaryKey val localDate: String,
    val waterCups: Int,
)
