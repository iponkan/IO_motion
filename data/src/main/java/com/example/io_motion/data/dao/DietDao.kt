package com.example.io_motion.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import com.example.io_motion.data.entity.DailyLogEntity
import com.example.io_motion.data.entity.DietEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DietDao {

    @Query("SELECT * FROM diet_entry_entities WHERE localDate = :localDate ORDER BY loggedAt ASC")
    fun observeEntries(localDate: String): Flow<List<DietEntryEntity>>

    @Query("SELECT * FROM daily_log_entities WHERE localDate = :localDate")
    fun observeDailyLog(localDate: String): Flow<DailyLogEntity?>

    @Insert
    suspend fun insertEntry(entry: DietEntryEntity)

    @Query("DELETE FROM diet_entry_entities WHERE id = :id")
    suspend fun deleteEntry(id: Long)

    @Upsert
    suspend fun upsertDailyLog(log: DailyLogEntity)
}
