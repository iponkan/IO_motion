package com.example.io_motion.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.example.io_motion.data.entity.RepEntity
import com.example.io_motion.data.entity.SessionEntity
import com.example.io_motion.data.entity.SessionWithReps
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    @Insert
    suspend fun insertSession(session: SessionEntity): Long

    @Insert
    suspend fun insertReps(reps: List<RepEntity>)

    @Transaction
    @Query("SELECT * FROM session_entities ORDER BY recordedAt DESC")
    fun observeAll(): Flow<List<SessionWithReps>>

    @Transaction
    @Query("SELECT * FROM session_entities WHERE id = :id")
    suspend fun getById(id: Long): SessionWithReps?

    @Query("DELETE FROM session_entities WHERE id = :id")
    suspend fun deleteById(id: Long)
}
