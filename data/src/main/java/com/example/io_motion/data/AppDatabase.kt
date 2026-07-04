package com.example.io_motion.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.io_motion.data.dao.SessionDao
import com.example.io_motion.data.entity.RepEntity
import com.example.io_motion.data.entity.SessionEntity

@Database(
    entities = [SessionEntity::class, RepEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
}
