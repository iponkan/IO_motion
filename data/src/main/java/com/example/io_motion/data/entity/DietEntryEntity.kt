package com.example.io_motion.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "diet_entry_entities",
    indices = [Index("localDate")],
)
data class DietEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** ISO local date `yyyy-MM-dd`. */
    val localDate: String,
    /** [com.example.io_motion.core.common.models.MealType] name (BREAKFAST|LUNCH|DINNER|SNACKS). */
    val mealType: String,
    val name: String,
    val kcal: Int,
    val loggedAt: Long,
)
